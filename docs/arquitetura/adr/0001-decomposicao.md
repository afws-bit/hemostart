# ADR 0001: Decomposição do Rota Vital

**Status:** Proposto

## Contexto
O Rota Vital é hoje um monolito Spring Boot com um único PostgreSQL, publicado como web service Docker no Render. As premissas de carga da Seção E vão de **5 req/s** (cenário baixo) a **300 req/s** (cenário alto). Pela Lei de Little (L = λ × W, com W = 0,3 s), isso dá 1,5, 12 e 90 requisições simultâneas, ou seja, **2, 2 e 9 instâncias** da API, com um pool de **1, 6 e 45 conexões** no banco. O custo mensal estimado no Render é de **US$ 97,00**, **US$ 149,25** e **US$ 556,25** (custo por pedido de US$ 0,0162, 0,0025 e 0,0009). O contexto Estoque concentra as regras críticas (ABO/Rh, validade, FEFO) e precisa de alta disponibilidade, e o squad tem 9 integrantes, prazo curto e orçamento limitado.

## Decisão
Projetamos quatro serviços alinhados aos bounded contexts: `estoque`, `requisicao`, `expedicao` e `telemetria`, cada um com seu banco. As consultas usam REST e as mudanças de estado usam eventos de domínio. O atendimento de uma requisição é uma saga coreografada com compensação: se a expedição falhar, o Estoque libera a reserva.

## Consequências
- **Melhora:** o Estoque escala e falha de forma independente dos demais contextos; cada contexto faz deploy separado; cada serviço usa o banco mais adequado.
- **Piora:** consistência eventual entre serviços; mais custo, porque cada serviço pago precisa de instância própria e de banco próprio (no Render, no mínimo uma instância de US$ 25/mês e um banco a partir de US$ 19/mês por serviço, valores da página de preços consultada em 18/11/2026); mais observabilidade e contratos de API e de eventos para manter; sagas difíceis de testar.
- **Alternativa descartada:** manter o monolito modular com escala horizontal. Ele atende bem até o cenário moderado (2 instâncias, US$ 149,25/mês), mas acopla o deploy e a disponibilidade do Estoque aos demais contextos no cenário alto (9 instâncias, 300 req/s).
