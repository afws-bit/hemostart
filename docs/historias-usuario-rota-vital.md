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
Funcionalidade: Relatório de Doações e Distribuições

  Cenário: Relatório do período com movimentações
    Dado que no período de "01/10/2026" a "31/10/2026" foram coletadas 12 bolsas
    E foram distribuídas 8 bolsas e descartadas 2 bolsas
    Quando o gestor gera o relatório desse período
    Então o sistema exibe "12" coletadas, "8" distribuídas e "2" descartadas

  Cenário: Relatório filtrado por tipo sanguíneo e hospital
    Dado que existem distribuições de bolsas "O-" e "A+" para hospitais diferentes
    Quando o gestor filtra o relatório pelo tipo "O-" e pelo hospital "Hospital Santa Cruz"
    Então apenas as movimentações de bolsas "O-" destinadas ao "Hospital Santa Cruz" são contabilizadas

  Cenário: Período sem movimentações
    Dado que não houve coletas, distribuições ou descartes no período selecionado
    Quando o gestor gera o relatório
    Então o sistema exibe a mensagem "Nenhum dado encontrado para o período selecionado."
```

---

## HU09 — Cadastro de Hospital

**Como** administrador do banco de sangue, **eu quero** cadastrar e gerenciar os hospitais solicitantes (razão social, CNPJ, endereço, contato e status), **para que** somente instituições autorizadas possam solicitar bolsas de sangue e a distribuição seja rastreável até o destino.

**Detalhes do negócio:**

- CNPJ é único no sistema (não pode haver duplicidade).
- Todo hospital possui status "ativo" ou "inativo".
- Hospitais inativos não podem realizar novas solicitações de sangue.

**Cenários (BDD):**

```gherkin
Funcionalidade: Cadastro de Hospital

  Cenário: Cadastro de hospital com dados válidos
    Dado que estou na tela de cadastro de hospital
    Quando informo razão social, CNPJ, endereço e contato válidos
    E confirmo o cadastro
    Então o hospital é salvo com status "ativo"
    E uma mensagem de confirmação é exibida

  Cenário: Tentativa de cadastro com CNPJ já existente
    Dado que já existe um hospital cadastrado com o CNPJ "12.345.678/0001-90"
    Quando tento cadastrar um novo hospital com o mesmo CNPJ
    Então o sistema exibe a mensagem "CNPJ já cadastrado"
    E o novo cadastro não é salvo

  Cenário: Solicitação de hospital inativo
    Dado que o hospital "Hospital São Lucas" está com status "inativo"
    Quando ele tenta enviar uma solicitação de sangue
    Então o sistema recusa a solicitação
    E exibe a mensagem "Hospital não autorizado"
```

## HU10 — Alerta de Estoque Crítico

**Como** gestor do banco de sangue, **eu quero** definir um nível mínimo de bolsas para cada tipo sanguíneo e ser alertado quando o estoque disponível ficar abaixo dele, **para que** eu possa organizar campanhas de doação e evitar a falta de sangue em atendimentos urgentes.

**Detalhes do negócio:**

- O nível mínimo é configurável por tipo sanguíneo (A+, A-, B+, B-, AB+, AB-, O+, O-).
- Apenas bolsas com status "disponível" contam para o nível; reservadas, vencidas e descartadas não entram na contagem.
- O alerta é removido automaticamente quando o estoque volta a atingir o nível mínimo.

**Cenários (BDD):**

```gherkin
Funcionalidade: Alerta de Estoque Crítico

  Cenário: Estoque abaixo do nível mínimo
    Dado que o nível mínimo do tipo "O-" é de 10 bolsas
    E existem apenas 6 bolsas "O-" disponíveis
    Quando o sistema executa a verificação de estoque
    Então o tipo "O-" é marcado com alerta de "estoque crítico"
    E o gestor é notificado

  Cenário: Estoque normalizado após nova coleta
    Dado que o tipo "O-" está com alerta de "estoque crítico"
    Quando novas coletas elevam o estoque disponível para 10 bolsas
    Então o alerta de "estoque crítico" é removido do tipo "O-"
```
## HU11 — Agendamento de Doação

**Como** atendente do banco de sangue, **eu quero** agendar uma doação para um doador cadastrado, escolhendo data e horário, **para que** o fluxo de coletas seja organizado e o doador só compareça quando estiver apto.

**Detalhes do negócio:**

- O agendamento só pode ser feito para doador cadastrado e apto na data escolhida (respeitando o intervalo mínimo de 60 dias para homens e 90 para mulheres).
- Cada horário possui um limite de vagas; ao atingi-lo, o horário não aceita novos agendamentos.
- O agendamento pode ser cancelado, e a vaga volta a ficar disponível.

**Cenários (BDD):**

```gherkin
Funcionalidade: Agendamento de Doação

  Cenário: Agendamento com doador apto e horário disponível
    Dado que o doador "Maria Silva" está apto para doação na data desejada
    E existem vagas no horário "09:00" do dia escolhido
    Quando registro o agendamento
    Então o agendamento é salvo com status "agendado"
    E uma mensagem de confirmação é exibida

  Cenário: Tentativa de agendamento em horário sem vagas
    Dado que o horário "09:00" já atingiu o limite de vagas
    Quando tento agendar uma nova doação nesse horário
    Então o sistema exibe a mensagem "Horário indisponível"
    E o agendamento não é salvo

  Cenário: Tentativa de agendamento antes do intervalo mínimo
    Dado que o doador "João Souza" doou sangue há 30 dias
    Quando tento agendar uma nova doação para ele
    Então o sistema exibe a mensagem "Doador ainda não apto para nova doação"
    E o agendamento não é salvo

  Cenário: Cancelamento de agendamento
    Dado que existe um agendamento com status "agendado"
    Quando o atendente cancela o agendamento
    Então o status muda para "cancelado"
    E a vaga do horário volta a ficar disponível
```

## HU12 — Triagem Clínica Pré-Doação

**Como** enfermeiro responsável pela triagem, **eu quero** registrar os dados clínicos do doador antes da coleta (peso, pressão arterial, temperatura e hemoglobina) e o sistema definir se ele está apto ou inapto, **para que** só doadores em condições seguras avancem para a coleta, protegendo doador e receptor.

**Detalhes do negócio:**

- A triagem deve ser registrada antes do registro de coleta (HU02).
- Hemoglobina mínima: 12,5 g/dL para mulheres e 13,0 g/dL para homens.
- Peso mínimo de 50kg e temperatura de até 37°C.
- Doador classificado como "inapto" não pode ter coleta registrada naquela visita.
- O motivo da inaptidão deve ser registrado.

**Cenários (BDD):**

```gherkin
Funcionalidade: Triagem Clínica Pré-Doação

  Cenário: Triagem com todos os parâmetros dentro do padrão
    Dado que o doador "Maria Silva" está na triagem
    Quando registro peso "62kg", temperatura "36.5" e hemoglobina "13.2"
    Então o resultado da triagem é "apto"
    E o doador fica liberado para o registro de coleta

  Cenário: Triagem com hemoglobina abaixo do mínimo
    Dado que a doadora "Ana Lima" está na triagem
    Quando registro hemoglobina "11.8"
    Então o resultado da triagem é "inapto"
    E o motivo "Hemoglobina abaixo do mínimo" é registrado

  Cenário: Tentativa de coleta com doador inapto
    Dado que o doador "João Souza" foi classificado como "inapto" na triagem
    Quando tento registrar uma coleta para ele
    Então o sistema exibe a mensagem "Doador inapto na triagem"
    E a coleta não é registrada
```

## HU13 — Aprovação e Recusa de Solicitações de Sangue

**Como** gestor do banco de sangue, **eu quero** analisar as solicitações pendentes dos hospitais e aprovar ou recusar cada uma, **para que** a distribuição só aconteça para pedidos avaliados e com bolsas compatíveis reservadas.

**Detalhes do negócio:**

- Apenas solicitações com status "pendente" podem ser aprovadas ou recusadas.
- Ao aprovar, o sistema reserva as bolsas compatíveis (seguindo HU04 e a política FEFO), e o status delas passa a "reservada".
- Se não houver bolsas suficientes, a aprovação é bloqueada.
- Toda recusa exige um motivo.

**Cenários (BDD):**

```gherkin
Funcionalidade: Aprovação de Solicitações

  Cenário: Aprovação de solicitação com estoque suficiente
    Dado que existe uma solicitação "pendente" de 3 bolsas "O-"
    E existem 5 bolsas "O-" disponíveis
    Quando o gestor aprova a solicitação
    Então o status da solicitação muda para "aprovada"
    E 3 bolsas passam para o status "reservada"

  Cenário: Aprovação com estoque insuficiente
    Dado que existe uma solicitação "pendente" de 3 bolsas "O-"
    E existem apenas 2 bolsas "O-" disponíveis
    Quando o gestor tenta aprovar a solicitação
    Então o sistema exibe a mensagem "Estoque insuficiente para atender a solicitação"
    E a solicitação continua "pendente"

  Cenário: Recusa de solicitação com motivo
    Dado que existe uma solicitação com status "pendente"
    Quando o gestor recusa a solicitação informando o motivo
    Então o status da solicitação muda para "recusada"
    E o motivo é registrado no histórico
```

## HU14 — Consulta de Histórico e Próxima Data Apta do Doador

**Como** atendente do banco de sangue, **eu quero** consultar o histórico de doações de um doador e ver a data em que ele poderá doar novamente, **para que** eu possa orientar o doador no balcão e evitar agendamentos ou coletas fora do intervalo mínimo.

**Detalhes do negócio:**

- O histórico lista as doações com data, tipo sanguíneo e volume.
- A próxima data apta é calculada a partir da última doação (60 dias para homens, 90 para mulheres).
- Doador sem doações anteriores aparece como "apto" imediatamente.

**Cenários (BDD):**

```gherkin
Funcionalidade: Histórico do Doador

  Cenário: Consulta de doador com doações anteriores
    Dado que o doador "João Souza" doou sangue há 30 dias
    Quando consulto o histórico dele
    Então vejo a lista de doações com data, tipo sanguíneo e volume
    E vejo a próxima data apta calculada em 60 dias após a última doação

  Cenário: Consulta de doador sem doações anteriores
    Dado que o doador "Carlos Pereira" nunca doou sangue
    Quando consulto o histórico dele
    Então o sistema informa que não há doações registradas
    E o doador aparece como "apto"
```




## Resumo das Histórias

| ID | História | Ator Principal |
|----|----------|----------------|
| HU01 | Cadastro de Doador | Atendente |
| HU02 | Registro de Coleta | Enfermeiro |
| HU03 | Controle de Estoque por Tipo Sanguíneo | Gestor |
| HU04 | Validação de Compatibilidade Sanguínea | Sistema |
| HU05 | Solicitação de Sangue por Hospital | Hospital |
| HU06 | Distribuição e Rastreio de Bolsas | Operador Logístico |
| HU07 | Controle de Validade e Descarte | Gestor de Qualidade |
| HU08 | Histórico e Relatório | Gestor |
| HU09 | Cadastro de Hospital | Administrador |
| HU10 | Alerta de Estoque Crítico | Gestor |
| HU11 | Agendamento de Doação | Atendente |
| HU12 | Triagem Clínica Pré-Doação | Enfermeiro |
| HU13 | Aprovação e Recusa de Solicitações | Gestor |
| HU14 | Histórico e Próxima Data Apta do Doador | Atendente |
