# ADR 0002: Uso de Mensageria para Comunicação entre Serviços

**Status:** Proposto

## Contexto
Com a separação dos serviços, a comunicação síncrona (REST) para o processo de reserva de sangue estava tornando o sistema lento e propenso a falhas em cascata. Se o serviço de Estoque caísse, as requisições travariam.

## Decisão
Decidimos adotar o RabbitMQ para comunicação assíncrona orientada a eventos entre os serviços de Requisição e Estoque, implementando o padrão Saga por Orquestração.

## Consequências
*   **Melhora:** Desacoplamento temporal. O serviço de requisição não precisa esperar o estoque responder para continuar funcionando.
*   **Piora:** A consistência dos dados passa a ser "eventual" (Eventual Consistency). Exige monitoramento mais complexo do broker de mensagens.
*   **Alternativa descartada:** Comunicação via REST síncrono. Descartada por gerar um alto acoplamento e falhas em cascata.