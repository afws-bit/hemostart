# ADR 0002: Comunicação assíncrona entre os serviços

**Status:** Proposto

## Contexto
A decomposição do ADR 0001 separa Estoque, Requisição, Expedição e Telemetria em serviços com banco próprio. O atendimento de uma requisição atravessa três deles (reservar a bolsa, despachar, marcar como atendida) e precisa de compensação se uma etapa falhar. As premissas de carga vão de 5 req/s a 300 req/s, com tempo médio de resposta de 0,3 s por serviço. O squad tem 9 integrantes e prazo curto.

## Decisão
O squad escolhe **eventos de domínio por fila** para as mudanças de estado (`RequisicaoCriada`, `BolsaReservada`, `ReservaLiberada`, `CadeiaFriaViolada`), em uma saga coreografada. O REST síncrono fica só para consultas (por exemplo, listar o estoque por tipo). Cada consumidor deve ser idempotente, porque um evento pode chegar mais de uma vez.

## Consequências
- **Melhora:** os serviços ficam desacoplados no tempo, a fila absorve picos, e a falha de um serviço não derruba o fluxo inteiro. A compensação (liberar a reserva) fica explícita no evento `ReservaLiberada`.
- **Piora:** consistência eventual, necessidade de idempotência, tratamento de ordem e de mensagens perdidas, e uma peça a mais de infraestrutura para operar e monitorar.
- **Alternativa descartada:** REST síncrono em cadeia. Com 0,3 s por serviço, três chamadas encadeadas levariam cerca de 0,9 s, e a indisponibilidade de um serviço bloquearia o pedido inteiro (acoplamento temporal e falha em cascata).
