# Sobe todo o ambiente de observabilidade do HemoStart, confere o checklist e mostra o acesso do professor.
# Uso no Windows (PowerShell), dentro da pasta observabilidade:
#   powershell -ExecutionPolicy Bypass -File .\subir.ps1
# Continue: o Docker escreve progresso no stderr, o que não é erro
$ErrorActionPreference = "Continue"
Set-Location -Path $PSScriptRoot

function Senha { -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 20 | ForEach-Object { [char]$_ }) }
function Valor($nome) {
    $linha = Get-Content .env | Where-Object { $_ -match "^$nome=" } | Select-Object -First 1
    if ($linha) { return $linha.Substring($nome.Length + 1) } else { return "" }
}

Write-Host "== 1/5 Conferindo o Docker"
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { Write-Host "Docker não encontrado. Instale o Docker Desktop."; exit 1 }
docker info *> $null
if ($LASTEXITCODE -ne 0) { Write-Host "O Docker não está rodando. Abra o Docker Desktop e espere ficar verde."; exit 1 }

Write-Host "== 2/5 Arquivo .env"
if (-not (Test-Path .env)) {
    $linhas = @(
        "DB_NAME=hemostart", "DB_USER=hemostart", "DB_PASSWORD=$(Senha)",
        "GRAFANA_ADMIN_USER=admin", "GRAFANA_ADMIN_PASSWORD=$(Senha)",
        "GRAFANA_VIEWER_USER=professor", "GRAFANA_VIEWER_PASSWORD=$(Senha)",
        "CARGA_INTERVALO_S=2"
    )
    # UTF-8 sem BOM, com quebra de linha LF (o Docker Compose lê melhor assim)
    [System.IO.File]::WriteAllText((Join-Path $PSScriptRoot ".env"), ($linhas -join "`n") + "`n")
    Write-Host "   Criei o .env com senhas aleatórias (ele NÃO vai para o GitHub)."
} else {
    Write-Host "   Usando o .env que já existe."
}

Write-Host "== 3/5 Subindo os contêineres (a primeira vez demora: compila a aplicação)"
docker compose --profile remoto up -d --build
if ($LASTEXITCODE -ne 0) { Write-Host "Falha ao subir. Copie a mensagem acima e mande para a equipe."; exit 1 }

Write-Host "== 4/5 Esperando a aplicação ficar saudável"
$estado = "?"
for ($i = 1; $i -le 90; $i++) {
    $estado = (docker inspect -f "{{.State.Health.Status}}" hemostart 2>$null)
    if ($estado -eq "healthy") { break }
    if ($i % 6 -eq 0) { Write-Host "   ...ainda subindo ($estado)" }
    Start-Sleep -Seconds 5
}
if ($estado -ne "healthy") { Write-Host "A aplicação não ficou saudável. Veja: docker compose logs hemostart"; exit 1 }
Write-Host "   Aplicação no ar. Aguardando 90 s para o gerador de carga produzir métricas, logs e traces..."
Start-Sleep -Seconds 90

Write-Host "== 5/5 Conferindo o checklist do projeto"
docker compose run --rm verificador
if ($LASTEXITCODE -ne 0) { Write-Host "   Algum item falhou: veja docs\evidencias\verificacao.txt" }

$url = ""
for ($i = 1; $i -le 12; $i++) {
    $logs = (docker compose logs cloudflared 2>&1) -join "`n"
    $m = [regex]::Matches($logs, "https://[a-z0-9-]+\.trycloudflare\.com")
    if ($m.Count -gt 0) { $url = $m[$m.Count - 1].Value; break }
    Start-Sleep -Seconds 5
}
if (-not $url) { $url = "<veja: docker compose logs cloudflared>" }

$acesso = @(
    "ACESSO DO PROFESSOR (não versionar; informar só no PDF/Classroom)",
    "Gerado em: $(Get-Date -Format 'dd/MM/yyyy HH:mm')",
    "URL:     $url",
    "Usuário: $(Valor 'GRAFANA_VIEWER_USER')",
    "Senha:   $(Valor 'GRAFANA_VIEWER_PASSWORD')",
    "",
    "ADMIN DA EQUIPE (local: http://localhost:3000)",
    "Usuário: $(Valor 'GRAFANA_ADMIN_USER')",
    "Senha:   $(Valor 'GRAFANA_ADMIN_PASSWORD')"
)
[System.IO.File]::WriteAllText((Join-Path $PSScriptRoot ".acesso-professor.txt"), ($acesso -join "`r`n") + "`r`n")

Write-Host ""
$acesso | ForEach-Object { Write-Host $_ }
Write-Host ""
Write-Host "Próximo passo: abra docs\evidencias\links-para-prints.md e tire os prints."
Write-Host "Mantenha esta máquina ligada durante a correção. Para desligar: docker compose --profile remoto down"
