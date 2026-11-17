package br.com.rotavital.model;

/** Situação de uma solicitação de sangue feita por um hospital. */
public enum StatusRequisicao {
    PENDENTE, APROVADA, ATENDIDA, CANCELADA;

    public String getChave() {
        return name().toLowerCase();
    }

    @Override
    public String toString() {
        return getChave();
    }
}
