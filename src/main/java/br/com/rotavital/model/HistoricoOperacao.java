package br.com.rotavital.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class HistoricoOperacao {

    private String id;
    private String tipo; // COLETA, RESERVA, DISTRIBUICAO, VENCIMENTO, DESCARTE
    private String codigoBolsa;
    private String descricao;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataHora;

    public HistoricoOperacao() {}

    public HistoricoOperacao(String tipo, String codigoBolsa, String descricao) {
        this.id = java.util.UUID.randomUUID().toString();
        this.tipo = tipo;
        this.codigoBolsa = codigoBolsa;
        this.descricao = descricao;
        this.dataHora = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getCodigoBolsa() { return codigoBolsa; }
    public void setCodigoBolsa(String codigoBolsa) { this.codigoBolsa = codigoBolsa; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
