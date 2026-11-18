package br.com.rotavital.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class TriagemClinica {

    private String id;
    private String doadorId;
    private double peso;
    private String pressaoArterial;
    private double temperatura;
    private double hemoglobina;
    private String resultado; // "apto", "inapto"
    private String motivoInaptidao;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHora;

    public TriagemClinica() {}

    public TriagemClinica(String id, String doadorId, double peso, String pressaoArterial,
                          double temperatura, double hemoglobina, String resultado, String motivoInaptidao) {
        this.id = id;
        this.doadorId = doadorId;
        this.peso = peso;
        this.pressaoArterial = pressaoArterial;
        this.temperatura = temperatura;
        this.hemoglobina = hemoglobina;
        this.resultado = resultado;
        this.motivoInaptidao = motivoInaptidao;
        this.dataHora = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoadorId() { return doadorId; }
    public void setDoadorId(String doadorId) { this.doadorId = doadorId; }

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }

    public String getPressaoArterial() { return pressaoArterial; }
    public void setPressaoArterial(String pressaoArterial) { this.pressaoArterial = pressaoArterial; }

    public double getTemperatura() { return temperatura; }
    public void setTemperatura(double temperatura) { this.temperatura = temperatura; }

    public double getHemoglobina() { return hemoglobina; }
    public void setHemoglobina(double hemoglobina) { this.hemoglobina = hemoglobina; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public String getMotivoInaptidao() { return motivoInaptidao; }
    public void setMotivoInaptidao(String motivoInaptidao) { this.motivoInaptidao = motivoInaptidao; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
