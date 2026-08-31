# Histórias de Usuário — Rota Vital (Sistema de Gestão de Banco de Sangue)

> Projeto Integrador — ADS 2026.2 — Grupo Hemostart
> Formato: User Story + Cenários de Validação em BDD (Gherkin)

---

## HU01 — Cadastro de Doador

**Como** atendente do banco de sangue,
**Eu quero** cadastrar os dados de um doador (nome, CPF, data de nascimento, tipo sanguíneo, contato e histórico de saúde básico),
**Para que** o sistema tenha um registro confiável de quem pode doar e possa controlar o intervalo mínimo entre doações.

**Detalhes de negócio:**
- CPF é único no sistema (não pode haver duplicidade).
- Idade mínima para doação: 16 anos (com autorização, conforme legislação) e máxima 69 anos (ou 60 se primeira doação).
- Peso mínimo de 50kg deve ser informado/validado no cadastro.

**Cenários (BDD):**

```gherkin
Funcionalidade: Cadastro de Doador

  Cenário: Cadastro de doador com dados válidos
    Dado que estou na tela de cadastro de doador
    Quando informo nome, CPF, data de nascimento, tipo sanguíneo e peso válidos
    E confirmo o cadastro
    Então o doador é salvo com sucesso no sistema
    E uma mensagem de confirmação é exibida

  Cenário: Tentativa de cadastro com CPF já existente
    Dado que já existe um doador cadastrado com o CPF "123.456.789-00"
    Quando tento cadastrar um novo doador com o mesmo CPF
    Então o sistema exibe a mensagem "CPF já cadastrado"
    E o novo cadastro não é salvo

  Cenário: Tentativa de cadastro com peso abaixo do mínimo permitido
    Dado que estou cadastrando um novo doador
    Quando informo o peso como "45kg"
    Então o sistema exibe a mensagem "Peso inferior ao mínimo permitido para doação"
    E o cadastro não é concluído
```

---

## HU02 — Registro de Coleta (Entrada de Bolsa de Sangue no Estoque)

**Como** enfermeiro responsável pela coleta,
**Eu quero** registrar uma nova bolsa de sangue coletada, vinculada ao doador, com tipo sanguíneo, volume, data de coleta e data de validade,
**Para que** a bolsa entre no estoque e possa ser rastreada até sua utilização ou descarte.

**Detalhes de negócio:**
- Cada bolsa recebe um código único (identificador) gerado pelo sistema.
- A data de validade é calculada automaticamente a partir da data de coleta e do tipo de componente (ex.: concentrado de hemácias válido por até 42 dias).
- Não é possível registrar coleta de doador que não cumpriu o intervalo mínimo desde a última doação (60 dias para homens, 90 para mulheres).

**Cenários (BDD):**

```gherkin
Funcionalidade: Registro de Coleta de Bolsa de Sangue

  Cenário: Registro de coleta válida
    Dado que o doador "Maria Silva" está apto para doação
    Quando registro uma nova coleta com tipo sanguíneo "O-" e volume "450ml"
    Então uma nova bolsa é criada com código único
    E a data de validade é calculada automaticamente
    E o estoque do tipo "O-" é atualizado

  Cenário: Tentativa de coleta antes do intervalo mínimo
    Dado que o doador "João Souza" doou sangue há 30 dias
    Quando tento registrar uma nova coleta para ele
    Então o sistema exibe a mensagem "Doador ainda não apto para nova doação"
    E a coleta não é registrada
```

---

## HU03 — Controle de Estoque por Tipo Sanguíneo

**Como** gestor do banco de sangue,
**Eu quero** visualizar a quantidade de bolsas disponíveis por tipo sanguíneo e status (disponível, reservada, vencida, descartada),
**Para que** eu possa tomar decisões rápidas sobre reposição e distribuição.

**Detalhes de negócio:**
- O painel deve exibir os 8 tipos sanguíneos (A+, A-, B+, B-, AB+, AB-, O+, O-).
- Bolsas vencidas devem mudar automaticamente de status e sair do estoque disponível.

**Cenários (BDD):**

```gherkin
Funcionalidade: Consulta de Estoque

  Cenário: Visualizar estoque atualizado por tipo sanguíneo
    Dado que existem 10 bolsas "A+" disponíveis e 2 "A+" vencidas
    Quando acesso o painel de estoque
    Então vejo "10 bolsas disponíveis" no tipo "A+"
    E as 2 bolsas vencidas aparecem separadamente como "vencidas"

  Cenário: Atualização automática de status por validade
    Dado que uma bolsa "B-" atinge a data de validade
    Quando o sistema executa a verificação diária de validade
    Então o status da bolsa muda para "vencida"
    E ela deixa de ser contabilizada no estoque disponível
```

---

## HU04 — Validação de Compatibilidade Sanguínea

**Como** sistema de gestão,
**Eu quero** validar automaticamente a compatibilidade entre o tipo sanguíneo solicitado por um paciente/hospital e as bolsas disponíveis,
**Para que** apenas bolsas compatíveis sejam sugeridas ou liberadas para distribuição, evitando erros que coloquem vidas em risco.

**Detalhes de negócio:**
- Regras de compatibilidade ABO/Rh devem seguir a tabela oficial de compatibilidade transfusional.
- O sistema deve sugerir a bolsa compatível mais próxima da data de validade (política FEFO — First Expire, First Out).

**Cenários (BDD):**

```gherkin
Funcionalidade: Validação de Compatibilidade

  Cenário: Solicitação com tipo sanguíneo compatível disponível
    Dado que um hospital solicita sangue tipo "O-" para um paciente
    E existem bolsas "O-" disponíveis em estoque
    Quando o sistema processa a solicitação
    Então uma bolsa compatível é sugerida
    E a bolsa com validade mais próxima do vencimento é priorizada

  Cenário: Solicitação com tipo incompatível
    Dado que um hospital tenta reservar uma bolsa "A+" para paciente "B-"
    Quando o sistema valida a compatibilidade
    Então a reserva é bloqueada
    E a mensagem "Tipo sanguíneo incompatível" é exibida
```

---

## HU05 — Solicitação de Sangue por Hospital

**Como** representante de um hospital cadastrado,
**Eu quero** solicitar bolsas de sangue de um tipo específico e quantidade, informando a urgência do pedido,
**Para que** o banco de sangue possa priorizar e organizar a distribuição conforme a necessidade clínica.

**Detalhes de negócio:**
- Toda solicitação deve estar vinculada a um hospital previamente cadastrado e autorizado.
- Pedidos marcados como "urgente" devem ser priorizados na fila de atendimento.

**Cenários (BDD):**

```gherkin
Funcionalidade: Solicitação de Sangue

  Cenário: Hospital realiza solicitação urgente
    Dado que o hospital "Hospital das Clínicas" está cadastrado e ativo
    Quando ele solicita 3 bolsas do tipo "O-" com prioridade "urgente"
    Então a solicitação é registrada com status "pendente"
    E ela aparece no topo da fila de atendimento

  Cenário: Solicitação de hospital não cadastrado
    Dado que um hospital não está cadastrado no sistema
    Quando ele tenta enviar uma solicitação de sangue
    Então o sistema recusa a solicitação
    E exibe a mensagem "Hospital não autorizado"
```

---

## HU06 — Distribuição e Rastreio de Bolsas para Hospitais

**Como** operador logístico do banco de sangue,
**Eu quero** registrar a saída de bolsas para um hospital solicitante, vinculando o código da bolsa ao pedido atendido,
**Para que** haja rastreabilidade completa de cada bolsa desde a coleta até a entrega final.

**Detalhes de negócio:**
- Ao confirmar a distribuição, o status da bolsa muda de "disponível" para "distribuída".
- O sistema deve registrar data, hora, responsável pela liberação e hospital destino.

**Cenários (BDD):**

```gherkin
Funcionalidade: Distribuição de Bolsas

  Cenário: Distribuição concluída com sucesso
    Dado que a solicitação do "Hospital São Lucas" foi aprovada com bolsa compatível reservada
    Quando o operador confirma a saída da bolsa
    Então o status da bolsa muda para "distribuída"
    E o histórico de rastreio é atualizado com data, hora e hospital destino

  Cenário: Tentativa de distribuir bolsa já utilizada
    Dado que a bolsa "BS-00123" já está com status "distribuída"
    Quando tento registrar uma nova distribuição para essa mesma bolsa
    Então o sistema bloqueia a ação
    E exibe a mensagem "Bolsa já distribuída anteriormente"
```

---

## HU07 — Controle de Validade e Descarte de Bolsas

**Como** gestor de qualidade do banco de sangue,
**Eu quero** que o sistema identifique automaticamente bolsas próximas do vencimento e as vencidas,
**Para que** eu possa agir preventivamente (priorizando o uso) ou realizar o descarte correto das bolsas vencidas.

**Detalhes de negócio:**
- Bolsas a até 5 dias do vencimento devem gerar alerta de "uso prioritário".
- Bolsas vencidas devem ser automaticamente movidas para status "descartada" após confirmação do responsável.

**Cenários (BDD):**

```gherkin
Funcionalidade: Controle de Validade

  Cenário: Alerta de bolsa próxima do vencimento
    Dado que uma bolsa "AB+" vence em 4 dias
    Quando o sistema executa a verificação diária
    Então a bolsa é marcada com alerta "uso prioritário"

  Cenário: Descarte de bolsa vencida
    Dado que uma bolsa "O+" está com status "vencida"
    Quando o responsável técnico confirma o descarte
    Então o status da bolsa muda para "descartada"
    E ela é removida do estoque disponível e urgente
```

---

## HU08 — Histórico e Relatório de Doações e Distribuições

**Como** gestor do banco de sangue,
**Eu quero** consultar relatórios com o histórico de doações recebidas e distribuições realizadas em um período,
**Para que** eu possa acompanhar indicadores de estoque, demanda e desempenho operacional.

**Detalhes de negócio:**
- O relatório deve permitir filtro por período, tipo sanguíneo e hospital.
- Deve exibir indicadores como total coletado, total distribuído e total descartado no período.

**Cenários (BDD):**

```gherkin
Funcionalidade: Relatórios de Doação e Distribuição

  Cenário: Geração de relatório mensal por tipo sanguíneo
    Dado que existem registros de coleta e distribuição no mês de agosto
    Quando filtro o relatório pelo tipo sanguíneo "A+" e período "01/08 a 31/08"
    Então o sistema exibe o total coletado, distribuído e descartado desse tipo no período

  Cenário: Relatório sem dados no período selecionado
    Dado que não há nenhum registro no período informado
    Quando gero o relatório
    Então o sistema exibe a mensagem "Nenhum dado encontrado para o período selecionado"
```

---

## Resumo das Histórias

| ID | História | Ator Principal |
|----|----------|-----------------|
| HU01 | Cadastro de Doador | Atendente |
| HU02 | Registro de Coleta | Enfermeiro |
| HU03 | Controle de Estoque por Tipo Sanguíneo | Gestor |
| HU04 | Validação de Compatibilidade Sanguínea | Sistema |
| HU05 | Solicitação de Sangue por Hospital | Hospital |
| HU06 | Distribuição e Rastreio de Bolsas | Operador Logístico |
| HU07 | Controle de Validade e Descarte | Gestor de Qualidade |
| HU08 | Histórico e Relatório | Gestor |
