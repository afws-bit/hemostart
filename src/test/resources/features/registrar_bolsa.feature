# language: pt

Funcionalidade: Reserva de Bolsa com regra FEFO

  Cenário: Reservar a bolsa com a validade mais próxima
    Dado que eu tenho a bolsa "B01" do tipo "O-" que vence em 10 dias
    E eu tenho a bolsa "B02" do tipo "O-" que vence em 2 dias
    Quando o hospital solicitar 1 bolsa do tipo "O-"
    Então o sistema deve reservar a bolsa "B02"
    E o status da bolsa "B02" deve ser "Reservada"
    E o status da bolsa "B01" deve permanecer "Disponível"