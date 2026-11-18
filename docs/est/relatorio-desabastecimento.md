# EST — Probabilidade de desabastecimento por tipo sanguíneo (Unidade 2)

> Projeto Integrador ADS 2026.2 — Equipe Hemostart — Sistema **Rota Vital**
> Ticket: PI3-84 · Código: `EstatisticaService` · Endpoint: `GET /api/estatisticas/desabastecimento` · Tela: **Relatórios → Risco de Desabastecimento**

## 1. Pergunta de negócio

*"Qual a chance de cada tipo sanguíneo ficar abaixo do nível mínimo de segurança na próxima semana?"*

O nível mínimo é o configurado pelo gestor na HU10, em Estoque → Configurar. Com esse número, o hemocentro decide **onde agir primeiro**: chamar doadores de um tipo, segurar distribuições não urgentes ou pedir apoio a outro hemocentro.

## 2. Método

Para cada tipo sanguíneo:

| Passo | O que é calculado | Fórmula |
|---|---|---|
| 1 | Demanda média diária (λ) | bolsas do tipo **distribuídas** nos últimos *J* dias ÷ *J* (padrão *J* = 30) |
| 2 | Demanda esperada no horizonte (μ) | μ = λ · *H* (padrão *H* = 7 dias) |
| 3 | Estoque útil (S) | bolsas **disponíveis** − bolsas que **vencem** dentro do horizonte |
| 4 | Folga até o mínimo (k) | k = S − mínimo configurado |
| 5 | Risco de desabastecimento | P(D > k), com D ~ **Poisson(μ)** → 1 − Σᵢ₌₀ᵏ e^(−μ) μⁱ / i! |

Regras extras:
- Se **k < 0**, o estoque útil já está abaixo do mínimo e o risco é **100%**.
- Se não houve demanda na janela (λ = 0), o risco é **0%**, desde que k ≥ 0.

**Classificação:**
- **BAIXO** (< 20%): acompanhamento normal.
- **MODERADO** (20% a 50%): atenção, avaliar uma campanha.
- **ALTO** (> 50%): ação imediata.

### Por que Poisson?
A demanda é uma **contagem de eventos** (bolsas pedidas) num intervalo de tempo. Os pedidos chegam de hospitais diferentes, de forma praticamente independente, e com uma taxa média estável em poucas semanas. Esse é o cenário clássico da distribuição de Poisson, que tem um único parâmetro (μ = média = variância) e é fácil de estimar com o histórico que o sistema já guarda. Para μ grande, a Poisson se aproxima de uma Normal(μ, √μ), mas aqui usamos a fórmula exata.

### Implementação
- O termo P(X = i) é calculado a partir do anterior (P(X = i) = P(X = i−1) · μ / i), sem calcular fatoriais. Isso evita estouro numérico e custa O(k).
- Os dados vêm do banco (PostgreSQL), por meio de `BolsaSangueRepository`, e o nível mínimo vem de `ConfiguracaoService`.
- Os testes estão em `EstatisticaDesabastecimentoTest`:
  - valores conhecidos: P(Poisson(2) > 2) = 0,3233 e P(Poisson(7) > 1) = 0,9927;
  - casos-limite;
  - um cenário completo gravado no banco.

## 3. Premissas e limitações

1. **Novas coletas não entram no horizonte.** A estimativa é conservadora: mede o risco *se nada for feito*.
2. **Bolsas que vencem no horizonte saem do estoque útil.** Também é conservador, porque na prática parte delas pode ser usada antes de vencer, e o FEFO prioriza justamente essas.
3. **A demanda é contada pelo tipo da bolsa distribuída,** não pelo tipo do paciente. Se o hospital pede A+ e recebe O−, a demanda entra no O−. Isso reflete o consumo real do estoque de cada tipo.
4. **A taxa é constante na janela.** Sazonalidade (feriados, campanhas, acidentes em massa) não é modelada. Com mais histórico, dá para usar uma janela por dia da semana ou um modelo com tendência.
5. **Os tipos são tratados de forma independente.** Pela compatibilidade, falta de A+ pode ser coberta por O+ ou O−, o que reduz o risco real dos tipos mais comuns e aumenta o dos universais.

## 4. Resultados — cenário ilustrativo

A tabela abaixo usa **dados de demonstração** (volumes realistas para um hemocentro de médio porte) para mostrar como ler o painel. Os números reais aparecem na tela **Relatórios** do sistema e mudam conforme as operações registradas.

Horizonte de 7 dias, janela de 30 dias:

| Tipo | Disponíveis | Vencem em 7 dias | Mínimo | Folga (k) | Demanda/dia (λ) | Demanda em 7 dias (μ) | Risco | Nível |
|---|---|---|---|---|---|---|---|---|
| O+ | 22 | 3 | 8 | 11 | 1,80 | 12,60 | 60,5% | ALTO |
| A+ | 15 | 1 | 6 | 8 | 1,30 | 9,10 | 55,7% | ALTO |
| O− | 6 | 1 | 5 | 0 | 0,70 | 4,90 | 99,3% | ALTO |
| B+ | 9 | 0 | 4 | 5 | 0,40 | 2,80 | 6,5% | BAIXO |
| A− | 7 | 2 | 3 | 2 | 0,30 | 2,10 | 35,0% | MODERADO |
| B− | 3 | 0 | 2 | 1 | 0,13 | 0,93 | 24,0% | MODERADO |
| AB+ | 6 | 1 | 2 | 3 | 0,17 | 1,17 | 3,1% | BAIXO |
| AB− | 2 | 0 | 1 | 1 | 0,07 | 0,47 | 8,0% | BAIXO |

## 5. Achados

1. **Ter mais bolsas não significa ter menos risco.**
   - O+ tem o maior estoque (22 bolsas), mas também a maior demanda (1,8 por dia). Por isso fica em risco ALTO (60,5%).
   - AB+, com só 6 bolsas, fica em risco BAIXO (3,1%), porque quase não é pedido.
   - O indicador correto é a **folga em relação à demanda**, não o estoque absoluto.
2. **O− é o ponto mais crítico.**
   - Com folga zero, basta **um** pedido na semana para ficar abaixo do mínimo, e a chance de haver pelo menos um pedido é de 99,3%.
   - Como o O− é doador universal, ele também é usado para cobrir a falta de outros tipos. O risco real é ainda maior que o calculado (premissa 5).
3. **O vencimento pesa no risco dos tipos negativos.** No A−, 2 das 7 bolsas vencem na semana. Sem elas, a folga cai de 4 para 2 e o risco passa de cerca de 6% para 35%. Isso reforça o uso do **FEFO** (AED), que consome primeiro as bolsas mais próximas do vencimento.
4. **Tipos raros têm risco MODERADO mesmo com pouca demanda,** como o B−. A folga é pequena (1 bolsa) e um único pedido já compromete o mínimo.

## 6. Recomendações

| Situação | Ação sugerida |
|---|---|
| Risco **ALTO** em O+ / A+ (alta demanda) | Campanha de doação direcionada (os doadores aptos aparecem em Doadores → Histórico/Aptidão; agendamento pela HU11) |
| Risco **ALTO** em O− | Reservar O− para urgências e pacientes O−; usar O+ para pacientes Rh+ sempre que possível |
| Risco **MODERADO** com bolsas vencendo | Priorizar essas bolsas nas próximas distribuições (FEFO já faz isso na aprovação) |
| Risco **BAIXO** em todos os tipos | Rever o nível mínimo, que pode estar alto demais e gerar descarte por vencimento |

## 7. Métrica de desperdício (revisada)

Antes, a média, a mediana e o desvio padrão de desperdício usavam o valor **1 para cada bolsa**. O resultado era sempre média 1 e desvio 0, ou seja, nenhuma informação.

Agora a amostra é a **quantidade de bolsas descartadas ou vencidas por semana nas últimas 8 semanas**:
- **média:** desperdício típico por semana;
- **mediana:** semana "normal", sem influência de picos;
- **desvio padrão amostral:** estabilidade do desperdício. Um valor alto indica semanas com perdas concentradas, que valem investigar (falha na verificação de validade, excesso de coleta de um tipo).

## 8. Como reproduzir

- **Tela:** Relatórios → card "Risco de Desabastecimento — próximos 7 dias".
- **API:** `GET /api/estatisticas/desabastecimento?horizonteDias=7&janelaDias=30` (os dois parâmetros podem ser alterados).
- **Testes:** `mvn test -Dtest=EstatisticaDesabastecimentoTest`.
