package br.com.rotavital.model;

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

    private String grupoAbo() {
        return rotulo.substring(0, rotulo.length() - 1);
    }

    /**
     * Regra ABO: O doa para todos; A doa para A e AB; B doa para B e AB; AB só para AB.
     * Regra Rh: Rh- doa para Rh- e Rh+; Rh+ só para Rh+.
     */
    public boolean podeDoarPara(TipoSanguineo receptor) {
        boolean aboOk = switch (grupoAbo()) {
            case "O" -> true;
            case "A" -> receptor.grupoAbo().equals("A") || receptor.grupoAbo().equals("AB");
            case "B" -> receptor.grupoAbo().equals("B") || receptor.grupoAbo().equals("AB");
            default -> receptor.grupoAbo().equals("AB");
        };
        boolean rhOk = isRhNegativo() || !receptor.isRhNegativo();
        return aboOk && rhOk;
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
