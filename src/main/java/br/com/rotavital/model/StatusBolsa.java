package br.com.rotavital.model;

/** Ciclo de vida de uma bolsa de sangue. */
public enum StatusBolsa {
    DISPONIVEL, RESERVADA, DISTRIBUIDA, VENCIDA, DESCARTADA;

    /** Valor em minúsculas usado nas telas e nas classes CSS (badge-disponivel etc.). */
    public String getChave() {
        return name().toLowerCase();
    }

    @Override
    public String toString() {
        return getChave();
    }
}
