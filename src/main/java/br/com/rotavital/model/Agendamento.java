package br.com.rotavital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** HU11 — agendamento de doação em um dia e horário. */
@Entity
@Table(name = "agendamento")
public class Agendamento extends EntidadeBase {

    public static final String AGENDADO = "agendado";
    public static final String CANCELADO = "cancelado";

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doador_id")
    private Doador doador;

    private LocalDate data;

    @Column(length = 5)
    private String horario; // ex: "09:00"

    @Column(length = 20)
    private String status = AGENDADO;

    private LocalDateTime dataCriacao;

    public Agendamento() {}

    public static Agendamento novo(Doador doador, LocalDate data, String horario) {
        Agendamento a = new Agendamento();
        a.doador = doador;
        a.data = data;
        a.horario = horario;
        a.status = AGENDADO;
        a.dataCriacao = LocalDateTime.now();
        return a;
    }

    public void cancelar() {
        this.status = CANCELADO;
    }

    public boolean isAgendado() {
        return AGENDADO.equals(status);
    }

    /** Usado pelas telas para mostrar o nome do doador. */
    public String getDoadorId() {
        return doador != null ? doador.getId() : null;
    }

    public Doador getDoador() { return doador; }
    public LocalDate getData() { return data; }
    public String getHorario() { return horario; }
    public String getStatus() { return status; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
