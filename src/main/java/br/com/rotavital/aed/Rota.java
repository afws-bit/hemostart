package br.com.rotavital.aed;

import java.util.List;

/** Resultado da roteirização. Equivale ao struct Rota de Grafo.cpp. */
public record Rota(List<String> paradas, int distanciaTotal, boolean encontrada) {

    public static Rota naoEncontrada() {
        return new Rota(List.of(), 0, false);
    }

    /** Ex.: "Hemocentro → Centro → Hospital Municipal Norte". */
    public String descricao() {
        return encontrada ? String.join(" → ", paradas) : "Rota não mapeada";
    }
}
