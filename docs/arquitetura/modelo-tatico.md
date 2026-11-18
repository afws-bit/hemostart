# Modelo Tático - Contexto Estoque (Core)

## Agregado: BolsaSanguinea
- **Raiz:** `BolsaSanguinea`, identificada pelo código único (`BS-xxxxx`). É a única entidade do agregado.
- **Objetos de valor:** `TipoSanguineo` (ABO/Rh), `DataValidade`, `StatusBolsa` (Disponível, Reservada, Distribuída, Expirada, Descartada).
- **Referência externa:** `Doador`, por ID.
- **Comportamentos da raiz:** `reservar(tipoReceptor, requisicaoId, hoje)`, `distribuir(hoje)`, `descartar(responsavel)`.

## Invariantes protegidas pela raiz
1. Só uma bolsa **Disponível** pode ser reservada.
2. Uma bolsa **vencida** (validade anterior a hoje) não pode ser reservada nem distribuída.
3. O tipo da bolsa deve ser **compatível** com o do receptor (ABO/Rh).
4. Somente a raiz altera o `StatusBolsa`.

## História de usuário âncora (invariantes 1 a 3)
Como **enfermeiro do estoque central**, quero **reservar uma bolsa compatível com o receptor**, para **garantir que o paciente receba o sangue correto e que a bolsa mais próxima do vencimento não seja desperdiçada**.

### Critérios de aceite
1. Bolsa Disponível, válida e compatível: a reserva muda o status para "Reservada" e vincula a bolsa à requisição.
2. Bolsa de tipo incompatível: a reserva é recusada com mensagem de erro e o status continua "Disponível".
3. Bolsa vencida: a reserva é recusada com mensagem de erro.
4. Bolsa já "Reservada": não pode ser reservada de novo.
5. Entre várias bolsas compatíveis e válidas, a sugerida é a de validade mais próxima (FEFO). *Política de seleção do serviço de aplicação, não do agregado.*

## Cenário Gherkin
Executado na Parte 3: [`reservar_bolsa.feature`](../../src/test/resources/features/reservar_bolsa.feature)