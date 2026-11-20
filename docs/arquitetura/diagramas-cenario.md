# Diagramas dos três cenários

Um diagrama por cenário, todos no Render (região Ohio), com os planos da planilha de orçamento. Os números vêm da aba Little: pico (λ), requisições simultâneas (L = λ × W, com W = 0,3 s), instâncias adotadas e pool de conexões.

## Cenário baixo: λ = 5 req/s, L = 1,5, 2 instâncias, pool de 1 conexão

![Cenário baixo](imgs/cenario-baixo.png)

```mermaid
flowchart LR
    U[Usuários e hospitais<br/>pico 5 req/s] --> LB[Balanceador<br/>incluso no Render]
    LB --> A1[API 1<br/>1c-2g]
    LB --> A2[API 2<br/>1c-2g]
    A1 --> DB[(PostgreSQL 0.5c-1g<br/>pool: 1 conexão)]
    A2 --> DB
```

## Cenário moderado: λ = 40 req/s, L = 12, 2 instâncias, pool de 6 conexões

![Cenário moderado](imgs/cenario-moderado.png)

```mermaid
flowchart LR
    U[Usuários e hospitais<br/>pico 40 req/s] --> LB[Balanceador<br/>incluso no Render]
    LB --> A1[API 1<br/>1c-2g]
    LB --> A2[API 2<br/>1c-2g]
    A1 --> DB[(PostgreSQL 0.5c-1g<br/>pool: 6 conexões)]
    A2 --> DB
    DB -. replicação .-> R[(Réplica / HA<br/>0.5c-1g)]
    A1 -. leituras do painel .-> C[Cache Key Value 256 MB]
    A2 -. leituras do painel .-> C
```

## Cenário alto: λ = 300 req/s, L = 90, 9 instâncias, pool de 45 conexões

![Cenário alto](imgs/cenario-alto.png)

```mermaid
flowchart LR
    U[Usuários e hospitais<br/>pico 300 req/s] --> LB[Balanceador<br/>incluso no Render]
    LB --> G[9 instâncias da API<br/>1c-2g cada]
    G --> DB[(PostgreSQL 1c-2g<br/>pool: 45 conexões)]
    DB -. replicação .-> R[(Réplica / HA<br/>1c-2g)]
    G -. leituras do painel .-> C[Cache Key Value 1 GB]
```

## O que muda de um cenário para o outro

| | Baixo | Moderado | Alto |
|---|---|---|---|
| Instâncias da API | 2 | 2 | 9 |
| Banco | `0.5c-1g` | `0.5c-1g` + réplica/HA | `1c-2g` + réplica/HA |
| Cache | não tem | Key Value 256 MB | Key Value 1 GB |
| Armazenamento do banco | 10 GB | 50 GB | 200 GB |
| Pool de conexões | 1 | 6 | 45 |
