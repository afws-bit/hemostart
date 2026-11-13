package br.com.rotavital.dto;

import java.util.Map;

public class EstatisticasGeraisDTO {
    private Map<String, Double> mediaBolsasPorTipo;
    private double taxaDesperdicio; // Em porcentagem
    private double tempoMedioEstoqueDias;

    public EstatisticasGeraisDTO(Map<String, Double> mediaBolsasPorTipo, double taxaDesperdicio, double tempoMedioEstoqueDias) {
        this.mediaBolsasPorTipo = mediaBolsasPorTipo;
        this.taxaDesperdicio = taxaDesperdicio;
        this.tempoMedioEstoqueDias = tempoMedioEstoqueDias;
    }

    // Getters
    public Map<String, Double> getMediaBolsasPorTipo() { return mediaBolsasPorTipo; }
    public double getTaxaDesperdicio() { return taxaDesperdicio; }
    public double getTempoMedioEstoqueDias() { return tempoMedioEstoqueDias; }
}