# Resultado do benchmarking de rede — HemoStart

- **Alvo:** `https://hemostart-rotavital-api.onrender.com`
- **Data:** 27/11/2026 00:36
- **Origem da medição:** Wi-Fi de casa, Recife
- **Sistema:** Darwin 24.3.0, Python 3.14.7
- **Parâmetros:** 50 amostras por medição, intervalo de 200 ms, carga de 15 s por nível em `/api/v1/estoque`

## Latência, jitter e perda

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

Primeira requisição (cold start): **6.674 ms** (fora das estatísticas acima).

## Vazão sob carga

| Clientes simultâneos | Requisições | Req/s | KB/s | Média (ms) | p95 (ms) | Perda |
|---|---|---|---|---|---|---|
| 1 | 56 | 3,68 | 4,5 | 271,8 | 451,2 | 0,00% |
| 5 | 163 | 10,60 | 13,0 | 468,6 | 748,8 | 0,00% |
| 10 | 245 | 15,73 | 19,3 | 626,4 | 1.096,0 | 0,00% |

## Observações automáticas

- Abrir uma conexão nova custa em média 187,9 ms (TCP 89,5 + TLS 98,4). Por isso navegadores e o script reaproveitam a conexão (keep-alive).
- Ping (ICMP) médio de 88,7 ms e conexão TCP de 89,5 ms: os dois medem basicamente o RTT até a borda; a diferença vem do processamento do SYN na borda.
- `GET /`: p95 de 916,8 ms, acima da meta de 800 ms.
- `GET /api/v1/estoque`: p95 de 606,9 ms, dentro da meta de 800 ms.
- `GET /api/v1/solicitacoes`: p95 de 465,7 ms, dentro da meta de 800 ms.
- `GET /api/v1/bolsas/alertas-validade`: p95 de 415,9 ms, dentro da meta de 800 ms.
- Maior perda entre os endpoints: 0,00% (meta ≤ 1%).
- Com 10 clientes a vazão foi 4,3x a de 1 cliente (3,68 → 15,73 req/s) e o p95 foi de 451,2 para 1.096,0 ms.

_Definições: latência = tempo até a resposta completa; jitter = média de |Lᵢ − Lᵢ₋₁|; perda = tentativas sem resposta válida (timeout, erro de conexão ou HTTP 5xx) ÷ total; percentis pelo posto mais próximo._
