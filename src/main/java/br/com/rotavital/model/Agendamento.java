package br.com.rotavital.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Agendamento {

    private String id;
    private String doadorId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate data;

    private String horario; // ex: "09:00"
    private String status; // "agendado", "cancelado"

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    public Agendamento() {}

    public Agendamento(String id, String doadorId, LocalDate data, String horario, String status) {
        this.id = id;
        this.doadorId = doadorId;
        this.data = data;
        this.horario = horario;
        this.status = status;
        this.dataCriacao = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoadorId() { return doadorId; }
    public void setDoadorId(String doadorId) { this.doadorId = doadorId; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
}
