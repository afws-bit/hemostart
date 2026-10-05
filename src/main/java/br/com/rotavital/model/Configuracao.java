package br.com.rotavital.model;

import java.util.HashMap;
import java.util.Map;

public class Configuracao {

    // mínimo de bolsas disponíveis por tipo sanguíneo
    private Map<String, Integer> minimoEstoque;

    public Configuracao() {
        minimoEstoque = new HashMap<>();
        for (String tipo : new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"}) {
            minimoEstoque.put(tipo, 5);
        }
    }

    public Map<String, Integer> getMinimoEstoque() { return minimoEstoque; }
    public void setMinimoEstoque(Map<String, Integer> minimoEstoque) { this.minimoEstoque = minimoEstoque; }
}
