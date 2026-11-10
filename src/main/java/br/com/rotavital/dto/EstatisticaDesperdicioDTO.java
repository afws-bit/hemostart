package br.com.rotavital.dto;

import java.util.Map;

public class EstatisticaDesperdicioDTO {
    private double mediaDesperdicio;
    private double medianaDesperdicio;
    private double desvioPadraoDesperdicio;
    private long totalBolsasDescartadas;
    private Map<String, Long> descartePorTipo;

    public EstatisticaDesperdicioDTO(double media, double mediana, double desvioPadrao, long total, Map<String, Long> descartePorTipo) {
        this.mediaDesperdicio = media;
        this.medianaDesperdicio = mediana;
        this.desvioPadraoDesperdicio = desvioPadrao;
        this.totalBolsasDescartadas = total;
        this.descartePorTipo = descartePorTipo;
    }

    // Getters
    public double getMediaDesperdicio() { return mediaDesperdicio; }
    public double getMedianaDesperdicio() { return medianaDesperdicio; }
    public double getDesvioPadraoDesperdicio() { return desvioPadraoDesperdicio; }
    public long getTotalBolsasDescartadas() { return totalBolsasDescartadas; }
    public Map<String, Long> getDescartePorTipo() { return descartePorTipo; }
}