package br.com.rotavital.aed;

import br.com.rotavital.model.BolsaSangue;

import java.util.List;

/** Equivale ao struct ResultadoAlocacao de Alocador.cpp. */
public record ResultadoAlocacao(List<BolsaSangue> bolsas, Rota rota, boolean atendido) {

    public static ResultadoAlocacao naoAtendido() {
        return new ResultadoAlocacao(List.of(), Rota.naoEncontrada(), false);
    }
}
