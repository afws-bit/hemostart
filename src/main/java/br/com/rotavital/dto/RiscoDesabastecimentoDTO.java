package br.com.rotavital.dto;

/**
 * EST — risco de um tipo sanguíneo ficar abaixo do nível mínimo no horizonte analisado.
 * probabilidade está entre 0 e 1; nivel é BAIXO (< 20%), MODERADO (20% a 50%) ou ALTO (> 50%).
 */
public record RiscoDesabastecimentoDTO(
        String tipoSanguineo,
        long disponiveis,
        long vencendoNoHorizonte,
        int nivelMinimo,
        double demandaMediaDiaria,
        double demandaEsperadaNoHorizonte,
        double probabilidade,
        String nivel) {
}
