# Três Cenários com Orçamento

> Os números abaixo são **exemplos de premissa**. Substituam pelos do squad e mantenham os mesmos nos ADRs.

## Premissas
| | Baixo | Moderado | Alto |
|---|---|---|---|
| Usuários | [100] | [1.000] | [10.000] |
| Pedidos por dia | [200] | [2.000] | [20.000] |
| Pico (λ, req/s) | [5] | [40] | [300] |
| Disponibilidade desejada | 99% | 99,5% | 99,9% |
| Tempo de resposta médio (W) | 0,3 s | 0,3 s | 0,3 s |
| Tempo no banco por requisição | 0,1 s | 0,1 s | 0,1 s |

## Lei de Little: L = λ · W
| | Baixo | Moderado | Alto |
|---|---|---|---|
| Requisições simultâneas (L = λ·W) | 5 × 0,3 = **1,5** | 40 × 0,3 = **12** | 300 × 0,3 = **90** |
| Capacidade por instância (premissa: 20 simultâneas, ~50% de uso) | 20 | 20 | 20 |
| Instâncias = ⌈L / 20⌉ | 1 → **2** (alta disponibilidade) | 1 → **2** | **5** |
| Conexões com o banco (λ · W_banco) | 5 × 0,1 = **0,5** | 40 × 0,1 = **4** | 300 × 0,1 = **30** |
| Pool dimensionado | 5 | 10 | 40 (8 por instância) |

## O que muda entre cenários
- **Baixo:** monolito em 2 instâncias, um banco PostgreSQL gerenciado.
- **Moderado:** mais memória e conexões, réplica de leitura, cache do painel de estoque.
- **Alto:** balanceador, 5 instâncias, réplica de leitura, e extração do Estoque como serviço (ADR 0002).

Diagrama de cada cenário: `diagramas/cenario-baixo.drawio`, `cenario-moderado.drawio`, `cenario-alto.drawio`.

## Orçamento (preencham a partir da página de preços do provedor)
| Cenário | Item | Qtd | Preço unitário (USD) | Custo mensal (USD) | Região | Fonte (URL) | Data da consulta |
|---|---|---|---|---|---|---|---|
| Baixo | Instância de aplicação | 2 | [ ] | [ ] | [ ] | [ ] | [dd/mm/2026] |
| Baixo | PostgreSQL gerenciado | 1 | [ ] | [ ] | [ ] | [ ] | [dd/mm/2026] |
| Baixo | **Total** | | | [ ] | | | |

Repitam o bloco para Moderado e Alto.

## Custo por pedido
`custo por pedido = custo mensal total / (pedidos por dia × 30)`

Preços on-demand, em dólar, de AWS, Azure, Google Cloud ou PaaS. Preço sugerido por IA não vale como fonte.