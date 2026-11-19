# Benchmarking de rede — HemoStart / Rota Vital

> **RSD · Unidade 2 · Critério "Benchmarking (latência/vazão/perda/jitter)" (40%)**
> Responsáveis: Elisa Martins Galindo Santos e Kelly Priscilla de Siqueira Melo · Revisão: 18/11/2026

## 1. Objetivo

Medir a qualidade da comunicação entre quem usa o sistema (navegador dos operadores e sistema dos hospitais) e a
aplicação publicada, nas quatro métricas pedidas: **latência, vazão, perda e jitter**. Os resultados servem de
**linha de base** para a migração descrita no [plano de implantação](plano-implantacao-migracao.md) (fases F0 e F5).

## 2. O que é medido, camada por camada

A requisição de um hospital atravessa várias camadas antes de chegar à aplicação. O script mede cada uma separadamente,
para mostrar **onde** o tempo é gasto:

| # | Camada | Medição | Ligação do diagrama |
|---|---|---|---|
| 1 | Aplicação (DNS) | Resolução do nome `hemostart-rotavital-api.onrender.com` | — |
| 2 | Rede (ICMP) | `ping` até a borda: RTT puro, quando o provedor responde a ICMP | Internet → borda |
| 3 | Transporte (TCP) | Tempo do *three-way handshake* na porta 443 | Internet → borda, TCP/443 |
| 4 | Segurança (TLS) | Tempo do *handshake* TLS depois da conexão TCP | Internet → borda, TLS 1.3 |
| 5 | Aplicação (HTTPS) | `GET` em 4 endpoints com conexão persistente (keep-alive) | Internet → borda → aplicação → banco |
| 6 | Carga | 1, 5 e 10 clientes simultâneos por 15 s em `/api/v1/estoque` | Cadeia inteira sob concorrência |

O *cold start* (primeira requisição, quando o plano gratuito do Render acorda o serviço) é medido e informado à parte,
para não distorcer as estatísticas.

## 3. Definições

| Métrica | Fórmula / critério |
|---|---|
| **Latência** | Tempo até receber a resposta completa (ms). Reportada como média, p50, p95, p99, mínimo e máximo |
| **Percentil pN** | Posto mais próximo: valor na posição ⌈N/100 · n⌉ da lista ordenada |
| **Jitter** | Média de \|Lᵢ − Lᵢ₋₁\| entre medições consecutivas (variação de atraso, como na RFC 3550) |
| **Perda** | Tentativas sem resposta válida (timeout, erro de conexão ou HTTP 5xx) ÷ total × 100 |
| **Vazão** | Requisições bem-sucedidas por segundo e KB/s de corpo de resposta recebidos |

As mesmas fórmulas estão no painel `/rede` (`EstatisticasRede.java`), então os números são comparáveis.

## 4. Como rodar (5 minutos)

Precisa só do **Python 3.8 ou mais novo** (nada para instalar). No Windows, se `python` não funcionar, tente `py`.

1. Baixe o repositório (ou só a pasta `docs/rsd/benchmark`).
2. Abra o terminal nessa pasta.
3. Rode, trocando a origem pelo seu caso:
   ```bash
   python benchmark_rede.py --origem "Wi-Fi de casa, Recife"
   ```
4. Espere terminar (cerca de 3 minutos, mais o tempo de acordar o Render).
5. Os arquivos ficam em `resultados/AAAA-MM-DD_HHMM/`:

| Arquivo | Para que serve |
|---|---|
| `relatorio.md` | Tabelas prontas, para colar na seção 6 deste documento |
| `amostras.csv` | Todas as medições brutas (evidência; abre no Excel) |
| `resumo.json` | Métricas calculadas |
| `benchmark-externo.json` | Copiar para `src/main/resources/static/rede/` para aparecer no painel |

Opções úteis: `--amostras 100` (mais precisão), `--duracao 30` (carga mais longa),
`--url http://localhost:8080` (medir a aplicação local, para comparar com o Render).

**Boas práticas para uma medição justa:** rodar duas vezes em horários diferentes; fechar downloads e vídeos;
anotar se foi Wi-Fi ou cabo, e a operadora. Para comparar cenários, repita com a mesma origem.

## 5. Ambiente de teste

| Item | Valor |
|---|---|
| Alvo | <https://hemostart-rotavital-api.onrender.com> (Render, plano gratuito, PostgreSQL 15 gerenciado) |
| Origem | _preencher: cidade, Wi-Fi ou cabo, operadora_ |
| Máquina | _preenchido automaticamente no relatorio.md (campo "Sistema")_ |
| Data e hora | _preenchido automaticamente_ |
| Parâmetros | 50 amostras por medição, intervalo de 200 ms; carga de 15 s com 1, 5 e 10 clientes |

## 6. Resultados

> **Pendente:** colar aqui as tabelas do `relatorio.md` depois de rodar o script.
> Os valores precisam ser medidos de verdade a partir de uma máquina com acesso à internet. Não usar números de exemplo.

### 6.1 Latência, jitter e perda

_(tabela do relatorio.md)_

### 6.2 Vazão sob carga

_(tabela do relatorio.md)_

## 7. Análise

Roteiro para a análise, depois de ter os números:

1. **Onde está o tempo?** Compare TCP + TLS (custo de abrir conexão) com o `GET` em conexão reaproveitada. Se a
   conexão nova custa mais que a requisição, o keep-alive é essencial.
2. **Rede ou servidor?** Latência HTTPS − RTT do TCP ≈ tempo de processamento (aplicação + banco). Confira com o
   card *Cliente → Aplicação* do painel `/rede`, que mede só o servidor.
3. **Estabilidade:** jitter alto em relação à média indica rede instável (Wi-Fi) ou servidor sobrecarregado.
4. **Escalabilidade:** se a vazão cresce quase em proporção aos clientes e o p95 sobe pouco, o sistema não está
   saturado; se a vazão para de crescer e o p95 dispara, achamos o limite.
5. **Metas:** compare com a seção 5 do plano de implantação (p95 HTTPS ≤ 800 ms, perda ≤ 1%, jitter ≤ 50 ms,
   ≥ 10 req/s com 10 clientes).
6. **Cold start:** registre o tempo de acordar e explique o impacto para o primeiro hospital do dia (motivação da Migração 2).

_(escrever a análise aqui)_

## 8. Conclusão

_(resumo em 3 a 5 linhas: as metas foram cumpridas? qual a principal limitação? o que muda na arquitetura-alvo?)_
