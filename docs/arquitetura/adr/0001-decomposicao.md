# ADR 0001: Decomposição do Rota Vital

**Status:** Proposto

## Contexto
O Rota Vital é hoje um monolito Spring Boot com um único PostgreSQL. A projeção de carga (ver `cenarios-orcamento.md`) vai de [5] req/s no cenário baixo a [300] req/s no alto. O contexto Estoque concentra as regras críticas (ABO/Rh, validade, FEFO) e tem perfil de carga diferente de Requisição e Expedição. O squad tem 8 integrantes, prazo curto e orçamento limitado, e o Estoque precisa de alta disponibilidade.

## Decisão
Projetamos quatro serviços alinhados aos bounded contexts: `estoque`, `requisicao`, `expedicao` e `telemetria`, cada um com seu banco, comunicando-se por REST nas consultas e por eventos nas mudanças de estado. O atendimento de uma requisição é uma saga coreografada com compensação (ver `projecao-microsservicos.md`).

## Consequências
- **Melhora:** o Estoque escala e falha de forma independente; deploy separado por contexto; banco adequado a cada serviço.
- **Piora:** consistência eventual, mais custo de infraestrutura e observabilidade, contratos de API e eventos para manter, sagas difíceis de testar.
- **Alternativa descartada:** monolito modular com escala horizontal. Atende bem até o cenário [moderado], mas acopla deploy e disponibilidade do Estoque aos demais contextos no cenário alto.