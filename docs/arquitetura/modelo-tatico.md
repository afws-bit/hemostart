# Modelo Tático - Contexto: Estoque

## Agregado: BolsaSanguinea
- **Raiz:** BolsaSanguinea
- **Entidades:** Doador (referenciado por ID)
- **Objetos de Valor:** TipoSanguineo (ABO/Rh), DataValidade, StatusBolsa
- **Invariante:** Uma bolsa só pode ser alterada para o status "Expedida" se estiver "Disponível" e se a `DataValidade` for maior que a data atual. A raiz do agregado é a única que pode alterar o `StatusBolsa`.

## História de Usuário Âncora
- **Persona:** Como enfermeiro do estoque central...
- **Ação:** ...quero reservar uma bolsa de sangue compatível...
- **Valor:** ...para garantir que o paciente receba o sangue correto e que a bolsa mais próxima do vencimento não seja desperdiçada.
- **Critérios de Aceite:**
  1. O sistema deve priorizar a bolsa com a data de validade mais próxima (FEFO).
  2. O tipo sanguíneo da bolsa deve ser compatível com o tipo do receptor.
  3. O status da bolsa deve mudar para "Reservada" imediatamente após a ação.