# Projeção com Microsserviços

## Serviços, comunicação e dados
| Serviço | Contexto | Banco próprio | Por que é um serviço |
|---|---|---|---|
| estoque | Estoque (core) | PostgreSQL | Regras críticas e maior carga de leitura; precisa escalar e falhar de forma isolada. |
| requisicao | Requisição | PostgreSQL | Fila de pedidos com ciclo de vida próprio. |
| expedicao | Expedição | PostgreSQL | Logística e rastreio, ritmo de mudança diferente. |
| telemetria | Telemetria | Banco de séries temporais | Alto volume de escrita e eventos. |

- **Consultas:** REST síncrono.
- **Mudanças de estado:** eventos de domínio por fila/broker.

## Saga: atender uma requisição (coreografada)
1. `requisicao` registra o pedido (**Pendente**) e publica `RequisicaoCriada`.
2. `estoque` escolhe a bolsa (FEFO), a reserva e publica `BolsaReservada`.
3. `expedicao` cria o despacho e publica `BolsaDespachada`.
4. `requisicao` marca o pedido como **Atendido**.

**Compensação:**
- Se `expedicao` falhar (ou `CadeiaFriaViolada`), publica `DespachoFalhou`.
- `estoque` libera a reserva (bolsa volta a **Disponível**) e publica `ReservaLiberada`.
- `requisicao` volta o pedido para **Pendente** ou **Falhou**.
- Se `estoque` não encontrar bolsa compatível, publica `ReservaRecusada` e a requisição fica **Pendente**.

## Serverless (opcional)
| Parte | Vira função? | Motivo |
|---|---|---|
| Emitir guia ao atender a requisição | Sim | Acionada por evento, sem estado, curta. |
| Processar leituras de telemetria | Sim | Eventos curtos e independentes. |
| Reserva de bolsa (Estoque) | Não | Exige transação e muitas conexões com o banco; cold start prejudica a latência. |
| Fila de requisições | Não | Mantém estado e ordem. |

## Comparação
| Critério | Monolito modular | Microsserviços | Serverless (parcial) |
|---|---|---|---|
| Unidade de deploy | Um artefato | Um por serviço | Por função |
| Escala | Tudo junto | Por serviço | Automática por evento |
| Consistência dos dados | Transação local (ACID) | Eventual, com sagas | Eventual |
| Custo | Menor em carga baixa | Maior (infra por serviço) | Paga por uso; bom em carga irregular |
| Complexidade de operação | Baixa | Alta (rede, tracing, contratos) | Média (limites, cold start, conexões) |