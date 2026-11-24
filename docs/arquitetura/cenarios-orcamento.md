# Três Cenários com Orçamento

Provedor: **Render (PaaS)**, região **Ohio (US East)**, a mesma do deploy atual. Preços on-demand em USD, sem free tier, reserva ou desconto, lidos pela equipe em https://render.com/pricing#compute em **18/11/2026** (capturas em `imgs/`). Os valores de carga são estimativas da equipe. A planilha completa está em `planilha_orcamento__.pdf`.

## Premissas
| | Baixo | Moderado | Alto |
|---|---|---|---|
| Usuários | 100 | 1.000 | 10.000 |
| Pedidos por dia | 200 | 2.000 | 20.000 |
| Pico (λ, req/s) | 5 | 40 | 300 |
| Disponibilidade desejada | 99% | 99,5% | 99,9% |
| Tempo de resposta médio (W) | 0,3 s | 0,3 s | 0,3 s |
| Tempo no banco por requisição | 0,1 s | 0,1 s | 0,1 s |
| Capacidade por instância (plano 1c-2g, 50% de uso) | 20 simult. (efetiva: 10) | 20 simult. (efetiva: 10) | 20 simult. (efetiva: 10) |

A capacidade de 20 requisições simultâneas por instância é uma estimativa, a confirmar com teste de carga no plano 1c-2g.

## Lei de Little: L = λ · W
| | Baixo | Moderado | Alto |
|---|---|---|---|
| Requisições simultâneas (L = λ · W) | 5 × 0,3 = **1,5** | 40 × 0,3 = **12** | 300 × 0,3 = **90** |
| Instâncias pela Lei de Little, ⌈L ÷ 10⌉ | 1 | 2 | 9 |
| Instâncias adotadas (mínimo de 2 para alta disponibilidade) | **2** | **2** | **9** |
| Conexões simultâneas com o banco (λ · 0,1 s) | 0,5 | 4 | 30 |
| Pool de conexões (× 1,5, arredondado para cima) | **1** | **6** | **45** |
| Limite de conexões do plano do banco | 100 | 100 | 100 |
| Indisponibilidade permitida no mês | 438 min | 219 min | 43,8 min |

## O que muda entre cenários
- **Baixo:** 2 instâncias da API (1c-2g) e PostgreSQL 0.5c-1g.
- **Moderado:** entram a réplica/HA do banco e o cache Key Value de 256 MB; mais armazenamento e tráfego.
- **Alto:** a API passa de 2 para 9 instâncias, o banco sobe para o plano 1c-2g com réplica, e o cache para 1 GB.

Diagramas: [`diagramas-cenario.md`](diagramas-cenario.md) e `imgs/cenario-baixo.png`, `imgs/cenario-moderado.png`, `imgs/cenario-alto.png`.

## Orçamento mensal (USD)
Todas as linhas: região Ohio (US East), fonte https://render.com/pricing#compute, consulta em 18/11/2026.

| Item | Preço unitário | Baixo | Moderado | Alto |
|---|---|---|---|---|
| Workspace Render Pro (necessário para mais de uma instância) | $25/mês | US$ 25,00 | US$ 25,00 | US$ 25,00 |
| Web service 1c-2g (1 CPU, 2 GB) | $25/mês | 2 → US$ 50,00 | 2 → US$ 50,00 | 9 → US$ 225,00 |
| PostgreSQL primário | 0.5c-1g $19 · 1c-2g $40 | US$ 19,00 | US$ 19,00 | US$ 40,00 |
| PostgreSQL réplica/HA * | mesmo plano do primário | — | US$ 19,00 | US$ 40,00 |
| Armazenamento do banco | $0,30/GB | 10 GB → US$ 3,00 | 50 GB → US$ 15,00 | 200 GB → US$ 60,00 |
| Balanceador de carga | incluso | US$ 0,00 | US$ 0,00 | US$ 0,00 |
| Tráfego de saída excedente (25 GB incluídos) | $0,15/GB | 0 GB → US$ 0,00 | 75 GB → US$ 11,25 | 975 GB → US$ 146,25 |
| Cache Key Value | 256 MB $10 · 1 GB $20 | — | US$ 10,00 | US$ 20,00 |
| Logs e monitoramento | incluso no Pro | US$ 0,00 | US$ 0,00 | US$ 0,00 |
| **Total mensal** | | **US$ 97,00** | **US$ 149,25** | **US$ 556,25** |
| Pedidos por mês (pedidos/dia × 30) | | 6.000 | 60.000 | 600.000 |
| **Custo por pedido** | | **US$ 0,0162** | **US$ 0,0025** | **US$ 0,0009** |

\* A página lista réplicas e alta disponibilidade, mas não informa o preço; assumimos o mesmo valor do plano do primário.

## Custo por pedido
`custo por pedido = custo mensal total / (pedidos por dia × 30)`

O custo total vai de US$ 97,00 a US$ 556,25, mas o custo por pedido cai de US$ 0,0162 para US$ 0,0009: há ganho de escala. No cenário alto, o tráfego de saída é o maior item depois das instâncias.

O deploy da disciplina roda no plano Free do Render (1 instância de 512 MB e PostgreSQL gratuito). Os três cenários acima são o orçamento de uma operação real e não descrevem o deploy atual.
