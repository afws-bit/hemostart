#!/bin/sh
# Cria (uma vez) o usuário de leitura do professor no Grafana, com papel Viewer.
# As credenciais vêm do .env, que não é versionado.
set -eu

echo "Aguardando o Grafana em ${GRAFANA_URL}..."
tentativas=0
until curl -sf "${GRAFANA_URL}/api/health" > /dev/null; do
  tentativas=$((tentativas + 1))
  if [ "$tentativas" -gt 60 ]; then
    echo "Grafana não respondeu em 2 minutos."
    exit 1
  fi
  sleep 2
done

corpo=$(printf '{"name":"Professor (leitura)","login":"%s","password":"%s"}' \
  "$GRAFANA_VIEWER_USER" "$GRAFANA_VIEWER_PASSWORD")

codigo=$(curl -s -o /tmp/resposta.json -w '%{http_code}' \
  -u "${GRAFANA_ADMIN_USER}:${GRAFANA_ADMIN_PASSWORD}" \
  -H 'Content-Type: application/json' \
  -X POST "${GRAFANA_URL}/api/admin/users" -d "$corpo")

case "$codigo" in
  200) echo "Usuário '${GRAFANA_VIEWER_USER}' criado com papel Viewer." ;;
  412) echo "Usuário '${GRAFANA_VIEWER_USER}' já existe; nada a fazer." ;;
  401) echo "Falha de login como admin: confira GRAFANA_ADMIN_USER/GRAFANA_ADMIN_PASSWORD no .env."; exit 1 ;;
  *)   echo "Resposta inesperada do Grafana (HTTP $codigo):"; cat /tmp/resposta.json; exit 1 ;;
esac
