package br.com.rotavital.model;

import br.com.rotavital.aed.MatrizCompatibilidade;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Os 8 tipos sanguíneos (ABO + Rh) e a tabela oficial de compatibilidade
 * transfusional de concentrado de hemácias. Encapsula a regra de quem pode
 * doar para quem — a regra mais crítica do sistema.
 */
public enum TipoSanguineo {

    A_POS("A+"), A_NEG("A-"), B_POS("B+"), B_NEG("B-"),
    AB_POS("AB+"), AB_NEG("AB-"), O_POS("O+"), O_NEG("O-");

    private final String rotulo;

    TipoSanguineo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean isRhNegativo() {
        return rotulo.endsWith("-");
    }

    /** Consulta a matriz de compatibilidade da AED (fonte única da regra ABO/Rh). */
    public boolean podeDoarPara(TipoSanguineo receptor) {
        return MatrizCompatibilidade.compativel(rotulo, receptor.rotulo);
    }

    public Set<TipoSanguineo> receptoresCompativeis() {
        Set<TipoSanguineo> receptores = EnumSet.noneOf(TipoSanguineo.class);
        for (TipoSanguineo t : values()) {
            if (podeDoarPara(t)) receptores.add(t);
        }
        return receptores;
    }

    public static TipoSanguineo deRotulo(String rotulo) {
        return Arrays.stream(values())
                .filter(t -> t.rotulo.equalsIgnoreCase(rotulo == null ? "" : rotulo.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo sanguíneo inválido: " + rotulo));
    }

    public static String[] rotulos() {
        return Arrays.stream(values()).map(TipoSanguineo::getRotulo).toArray(String[]::new);
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
