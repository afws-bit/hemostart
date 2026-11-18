# ADR 0002: Escolha do squad para a Parte 3

**Status:** Proposto

## Contexto
Prazo curto, 8 integrantes, necessidade de entregar a história âncora com Gherkin executável. O deploy atual é um único container no Render.

## Decisão
Implementar a Parte 3 como **monolito modular**, com pacotes por contexto e por camada. O Estoque só será extraído como serviço se [condição mensurável, ex.: pico acima de X req/s].

## Consequências
- **Melhora:** menos infraestrutura, transações locais, deploy simples.
- **Piora:** escala conjunta dos contextos até a extração.
- **Alternativa descartada:** adotar microsserviços já agora, pelo custo operacional e pelo prazo.