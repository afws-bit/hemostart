# language: pt
Funcionalidade: Reservar bolsa de sangue
  Como enfermeiro do estoque central
  Quero reservar uma bolsa compatível com o receptor
  Para garantir o sangue correto e evitar desperdício

  Cenário: Reservar bolsa compatível e válida
    Dado uma bolsa "BS-00001" do tipo "O-" com status "Disponível" e validade futura
    E um receptor do tipo "A+"
    Quando eu reservo a bolsa "BS-00001" para a requisição "REQ-001"
    Então o status da bolsa deve ser "Reservada"
    E a bolsa deve estar vinculada à requisição "REQ-001"

  Cenário: Bolsa incompatível é recusada
    Dado uma bolsa "BS-00002" do tipo "A+" com status "Disponível" e validade futura
    E um receptor do tipo "O-"
    Quando eu tento reservar a bolsa "BS-00002"
    Então a reserva deve ser recusada por incompatibilidade
    E o status da bolsa deve continuar "Disponível"

  Cenário: Bolsa vencida é recusada
    Dado uma bolsa "BS-00003" do tipo "O-" com validade anterior a hoje
    Quando eu tento reservar a bolsa "BS-00003"
    Então a reserva deve ser recusada por vencimento