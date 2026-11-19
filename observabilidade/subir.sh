#!/usr/bin/env bash
# Sobe todo o ambiente de observabilidade do HemoStart, confere o checklist e mostra o acesso do professor.
# Uso (Linux, macOS ou WSL):  cd observabilidade && bash subir.sh
set -euo pipefail
cd "$(dirname "$0")"

echo "== 1/5 Conferindo o Docker"
command -v docker >/dev/null || { echo "Docker não encontrado. Instale o Docker e rode de novo."; exit 1; }
docker compose version >/dev/null || { echo "Docker Compose v2 não encontrado."; exit 1; }
docker info >/dev/null 2>&1 || { echo "O Docker não está rodando. Abra o Docker Desktop (ou: sudo systemctl start docker)."; exit 1; }

senha() { LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 20; }

echo "== 2/5 Arquivo .env"
if [ ! -f .env ]; then
  cat > .env <<EOF
DB_NAME=hemostart
DB_USER=hemostart
DB_PASSWORD=$(senha)
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=$(senha)
GRAFANA_VIEWER_USER=professor
GRAFANA_VIEWER_PASSWORD=$(senha)
CARGA_INTERVALO_S=2
EOF
  echo "   Criei o .env com senhas aleatórias (ele NÃO vai para o GitHub)."
else
  echo "   Usando o .env que já existe."
fi
valor() { grep -E "^$1=" .env | head -1 | cut -d= -f2-; }

echo "== 3/5 Subindo os contêineres (a primeira vez demora: compila a aplicação)"
docker compose --profile remoto up -d --build

echo "== 4/5 Esperando a aplicação ficar saudável"
for i in $(seq 1 90); do
  estado=$(docker inspect -f '{{.State.Health.Status}}' hemostart 2>/dev/null || echo "?")
  [ "$estado" = "healthy" ] && break
  [ $((i % 6)) -eq 0 ] && echo "   ...ainda subindo ($estado)"
  sleep 5
done
[ "$estado" = "healthy" ] || { echo "A aplicação não ficou saudável. Veja: docker compose logs hemostart"; exit 1; }
echo "   Aplicação no ar. Aguardando 90 s para o gerador de carga produzir métricas, logs e traces..."
sleep 90

echo "== 5/5 Conferindo o checklist do projeto"
docker compose run --rm verificador || echo "   Algum item falhou: veja docs/evidencias/verificacao.txt"

URL=""
for i in $(seq 1 12); do
  URL=$(docker compose logs cloudflared 2>&1 | grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' | tail -1 || true)
  [ -n "$URL" ] && break
  sleep 5
done

{
  echo "ACESSO DO PROFESSOR (não versionar; informar só no PDF/Classroom)"
  echo "Gerado em: $(date '+%d/%m/%Y %H:%M')"
  echo "URL:     ${URL:-<veja: docker compose logs cloudflared>}"
  echo "Usuário: $(valor GRAFANA_VIEWER_USER)"
  echo "Senha:   $(valor GRAFANA_VIEWER_PASSWORD)"
  echo
  echo "ADMIN DA EQUIPE (local: http://localhost:3000)"
  echo "Usuário: $(valor GRAFANA_ADMIN_USER)"
  echo "Senha:   $(valor GRAFANA_ADMIN_PASSWORD)"
} > .acesso-professor.txt

echo
cat .acesso-professor.txt
echo
echo "Próximo passo: abra docs/evidencias/links-para-prints.md e tire os prints."
echo "Mantenha esta máquina ligada durante a correção. Para desligar: docker compose --profile remoto down"
