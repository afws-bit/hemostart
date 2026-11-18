# Glossário da Linguagem Ubíqua

| # | Termo | Contexto | Definição |
|---|---|---|---|
| 1 | Bolsa | Estoque | Volume de hemocomponente armazenado, com código único, tipo sanguíneo e data de validade. |
| 2 | Bolsa | Expedição | Item logístico a ser embalado, etiquetado e despachado com acondicionamento térmico até o hospital. |
| 3 | Doador | Estoque | Pessoa que doa sangue após a triagem; a bolsa o referencia apenas por ID. |
| 4 | Coleta | Estoque | Extração do sangue do doador e criação da bolsa no sistema. |
| 5 | Validade | Estoque | Data limite de uso da bolsa, calculada pelo tipo de componente. |
| 6 | FEFO | Estoque | Regra pela qual a bolsa com validade mais próxima é usada primeiro. |
| 7 | Compatibilidade ABO/Rh | Estoque | Regra que define se o tipo da bolsa pode ser transfundido no receptor. |
| 8 | Reserva | Estoque | Separação de uma bolsa para uma requisição, impedindo que outros a utilizem. |
| 9 | Uso Prioritário | Estoque | Alerta de bolsa com 5 dias ou menos para vencer. |
| 10 | Descarte | Estoque | Baixa de bolsa vencida ou contaminada, com registro do responsável. |
| 11 | Requisição | Requisição | Pedido de sangue de um hospital, com tipo, quantidade e urgência. |
| 12 | Hospital Requisitante | Requisição | Hospital ativo e cadastrado autorizado a solicitar sangue. |
| 13 | Urgência | Requisição | Nível de prioridade que coloca a requisição no topo da fila. |
| 14 | Despacho | Expedição | Saída física da bolsa rumo ao hospital, com rota e responsável. |
| 15 | Leitura de Temperatura | Telemetria | Medição da temperatura da bolsa durante o transporte. |
| 16 | Violação da Cadeia Fria | Telemetria | Leitura fora da faixa permitida, que torna a bolsa imprópria para uso. |

> "Bolsa" tem sentidos diferentes em Estoque (o hemocomponente) e Expedição (o pacote logístico).