package br.com.rotavital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * HU12 — triagem clínica pré-doação. O próprio objeto avalia os sinais vitais
 * e define se o doador está apto (regra de negócio dentro da entidade).
 */
@Entity
@Table(name = "triagem_clinica")
public class TriagemClinica extends EntidadeBase {

    public static final String APTO = "apto";
    public static final String INAPTO = "inapto";

    public static final double HEMOGLOBINA_MINIMA_MULHER = 12.5;
    public static final double HEMOGLOBINA_MINIMA_HOMEM = 13.0;
    public static final double TEMPERATURA_MAXIMA = 37.0;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doador_id")
    private Doador doador;

    private double peso;

    @Column(length = 10)
    private String pressaoArterial;

    private double temperatura;
    private double hemoglobina;

    @Column(length = 10)
    private String resultado;

    private String motivoInaptidao;
    private LocalDateTime dataHora;

    public TriagemClinica() {}

    /** Registra os sinais vitais e já calcula o resultado. */
    public static TriagemClinica avaliar(Doador doador, double peso, String pressaoArterial,
                                         double temperatura, double hemoglobina) {
        TriagemClinica t = new TriagemClinica();
        t.doador = doador;
        t.peso = peso;
        t.pressaoArterial = pressaoArterial;
        t.temperatura = temperatura;
        t.hemoglobina = hemoglobina;
        t.dataHora = LocalDateTime.now();

        double hbMinima = "F".equalsIgnoreCase(doador.getSexo()) ? HEMOGLOBINA_MINIMA_MULHER : HEMOGLOBINA_MINIMA_HOMEM;
        if (hemoglobina < hbMinima) {
            t.motivoInaptidao = "Hemoglobina abaixo do mínimo";
        } else if (peso < Doador.PESO_MINIMO) {
            t.motivoInaptidao = "Peso inferior ao mínimo permitido";
        } else if (temperatura > TEMPERATURA_MAXIMA) {
            t.motivoInaptidao = "Temperatura corporal acima do limite permitido (37°C)";
        }
        t.resultado = t.motivoInaptidao == null ? APTO : INAPTO;
        return t;
    }

    public boolean isApto() {
        return APTO.equals(resultado);
    }

    public String getDoadorId() {
        return doador != null ? doador.getId() : null;
    }

    public Doador getDoador() { return doador; }
    public double getPeso() { return peso; }
    public String getPressaoArterial() { return pressaoArterial; }
    public double getTemperatura() { return temperatura; }
    public double getHemoglobina() { return hemoglobina; }
    public String getResultado() { return resultado; }
    public String getMotivoInaptidao() { return motivoInaptidao; }
    public LocalDateTime getDataHora() { return dataHora; }
}
