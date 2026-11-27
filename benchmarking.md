# Benchmarking de rede — HemoStart / Rota Vital

> **RSD · Unidade 2 · Critério "Benchmarking (latência/vazão/perda/jitter)" (40%)**
> Responsáveis: Elisa Martins Galindo Santos e Kelly Priscilla de Siqueira Melo · Revisão: 27/11/2026

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
| Alvo | <https://hemostart-rotavital-api.onrender.com> (Render, plano gratuito, região Ohio/EUA, PostgreSQL 18 gerenciado) |
| Origem | Wi-Fi de casa, Recife |
| Máquina | MacBook Pro, macOS (Darwin 24.3.0), Python 3.14.7 |
| Data e hora | 27/11/2026, 00:36 |
| Parâmetros | 50 amostras por medição, intervalo de 200 ms; carga de 15 s com 1, 5 e 10 clientes em `/api/v1/estoque` |

Arquivos brutos desta medição: [`benchmark/resultados/2026-11-27_0036/`](benchmark/resultados/2026-11-27_0036/)
(`amostras.csv`, `resumo.json` e `relatorio.md`).

## 6. Resultados

### 6.1 Latência, jitter e perda

| Medição | Média (ms) | p50 (ms) | p95 (ms) | Jitter (ms) | Perda | Sucessos |
|---|---|---|---|---|---|---|
| Resolução DNS | 2,7 | 2,3 | 4,3 | 0,7 | 0,00% | 20/20 |
| ICMP ping | 88,7 | 87,5 | 102,9 | 5,4 | 10,00% | 18/20 |
| Conexão TCP :443 | 89,5 | 87,9 | 101,3 | 4,0 | 0,00% | 30/30 |
| Handshake TLS | 98,4 | 90,2 | 103,6 | 21,1 | 0,00% | 30/30 |
| HTTPS GET `/` | 499,9 | 454,3 | 916,8 | 133,6 | 0,00% | 50/50 |
| HTTPS GET `/api/v1/estoque` | 325,9 | 240,3 | 606,9 | 120,3 | 0,00% | 50/50 |
| HTTPS GET `/api/v1/solicitacoes` | 247,4 | 223,0 | 465,7 | 40,3 | 0,00% | 50/50 |
| HTTPS GET `/api/v1/bolsas/alertas-validade` | 245,6 | 222,5 | 415,9 | 36,5 | 0,00% | 50/50 |

Primeira requisição desta rodada (*cold start*): **6.674 ms**, fora das estatísticas acima.

### 6.2 Vazão sob carga

| Clientes simultâneos | Requisições | Req/s | KB/s | Média (ms) | p95 (ms) | Perda |
|---|---|---|---|---|---|---|
| 1 | 56 | 3,68 | 4,5 | 271,8 | 451,2 | 0,00% |
| 5 | 163 | 10,60 | 13,0 | 468,6 | 748,8 | 0,00% |
| 10 | 245 | 15,73 | 19,3 | 626,4 | 1.096,0 | 0,00% |

## 7. Análise

**Onde está o tempo.** Abrir uma conexão nova custa em média 187,9 ms (TCP 89,5 + TLS 98,4). Uma requisição em
conexão reaproveitada leva de 245 a 500 ms. A conexão nova é quase metade do custo de uma requisição, então o
keep-alive do navegador e do script é importante.

**Rede ou servidor.** O RTT até a borda é de cerca de 89 ms (ping 88,7 ms e TCP 89,5 ms), coerente com uma ida e volta
de Recife até a região de Ohio. Subtraindo esse RTT da latência HTTPS, sobra o tempo de processamento: cerca de
158 ms em `/api/v1/solicitacoes` e `/api/v1/bolsas/alertas-validade`, 236 ms em `/api/v1/estoque` e 411 ms em `/`,
a página inicial. A maior parte do tempo está no servidor (aplicação e banco), não na rede.

**Estabilidade.** A rede foi estável: jitter de 4,0 ms no TCP e 5,4 ms no ping. A variação aparece no servidor: o
jitter é de 133,6 ms em `/` e de 120,3 ms em `/api/v1/estoque`, contra cerca de 40 ms nos outros dois endpoints.
A perda de 10% no ping (2 de 20) não afetou o serviço: TCP, TLS e HTTPS tiveram 0% de perda. O ICMP costuma ter
prioridade baixa ou limite de taxa na borda, por isso ele não é usado para avaliar a meta de perda.

**Escalabilidade.** De 1 para 5 clientes a vazão quase triplicou (3,68 → 10,60 req/s) e o p95 subiu de 451 para 749 ms.
De 5 para 10 clientes ela cresceu só 1,5 vez (15,73 req/s) e o p95 passou de 1 segundo (1.096 ms). O sistema começa
a saturar perto de 10 clientes simultâneos, o que é esperado com uma única instância do plano gratuito.

**Metas** (seção 5 do [plano de implantação](plano-implantacao-migracao.md)):

| Meta | Resultado | Situação |
|---|---|---|
| p95 HTTPS ≤ 800 ms | 415,9 a 606,9 ms nos três endpoints da API; 916,8 ms em `/` | Cumprida na API; `/` acima da meta, mas abaixo do nível crítico (2.000 ms) |
| Perda ≤ 1% | 0,00% em TCP, TLS e HTTPS | Cumprida |
| Jitter ≤ 50 ms | 36,5 e 40,3 ms em dois endpoints; 120,3 e 133,6 ms em `/api/v1/estoque` e `/` | Parcial |
| Vazão ≥ 10 req/s com 10 clientes | 15,73 req/s | Cumprida |

**Cold start.** Nesta rodada a primeira requisição levou 6,7 s. Numa tentativa anterior na mesma noite, com o serviço
parado por inatividade, a aplicação levou cerca de 152 s para iniciar: o log do Render registra
"Started RotaVitalApplication in 151.698 seconds". Isso passou do limite de 120 s do script, e a medição só foi
possível na segunda tentativa. Para um hospital, isso significa que o primeiro pedido do dia pode esperar
mais de 2 minutos. É a principal motivação da migração para um servidor sempre ligado.

## 8. Conclusão

Com o serviço acordado, o HemoStart cumpre as metas de perda (0%) e de vazão (15,73 req/s com 10 clientes), e a API
fica dentro do p95 de 800 ms. Os pontos fracos são a página inicial (p95 de 917 ms), o jitter alto em `/` e em
`/api/v1/estoque`, a saturação perto de 10 clientes e, principalmente, o *cold start* de até cerca de 2,5 minutos
do plano gratuito. A arquitetura-alvo do plano de implantação (instância sempre ligada, banco na sub-rede de dados
e mais de uma instância da aplicação) ataca exatamente esses pontos. Esta medição fica como linha de base (fase F0)
para comparar na fase F5.
