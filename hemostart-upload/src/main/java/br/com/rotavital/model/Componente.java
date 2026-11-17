package br.com.rotavital.model;

import java.util.Arrays;

/** Hemocomponentes e seus prazos de validade (em dias) a partir da coleta. */
public enum Componente {

    CONCENTRADO_HEMACIAS("Concentrado de Hemácias", 42),
    PLAQUETAS("Plaquetas", 5),
    PLASMA("Plasma", 365);

    private final String descricao;
    private final int diasValidade;

    Componente(String descricao, int diasValidade) {
        this.descricao = descricao;
        this.diasValidade = diasValidade;
    }

    public String getDescricao() { return descricao; }
    public int getDiasValidade() { return diasValidade; }

    public static Componente deDescricao(String descricao) {
        return Arrays.stream(values())
                .filter(c -> c.descricao.equalsIgnoreCase(descricao))
                .findFirst()
                .orElse(CONCENTRADO_HEMACIAS);
    }

    public static String[] descricoes() {
        return Arrays.stream(values()).map(Componente::getDescricao).toArray(String[]::new);
    }
}
