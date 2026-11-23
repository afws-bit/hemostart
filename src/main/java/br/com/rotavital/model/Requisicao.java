package br.com.rotavital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "requisicao")
public class Requisicao extends EntidadeBase {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @Column(length = 3)
    private String tipoSanguineo;

    private int quantidade;
    private boolean urgente;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatusRequisicao status = StatusRequisicao.PENDENTE;

    private LocalDateTime dataSolicitacao;
    private LocalDateTime dataAtendimento;
    private String responsavelDistribuicao;

    /** Rota calculada pela roteirização (AED U2) na hora da reserva. */
    @Column(length = 500)
    private String rotaEntrega;

    private Integer distanciaKm;

    /** HU13 — motivo informado ao recusar a solicitação. */
    @Column(length = 500)
    private String motivoRecusa;

    public Requisicao() {}

    public static Requisicao nova(Hospital hospital, String tipoSanguineo, int quantidade, boolean urgente) {
        Requisicao r = new Requisicao();
        r.hospital = hospital;
        r.tipoSanguineo = tipoSanguineo;
        r.quantidade = quantidade;
        r.urgente = urgente;
        r.status = StatusRequisicao.PENDENTE;
        r.dataSolicitacao = LocalDateTime.now();
        return r;
    }

    // ===================== Regras de negócio =====================

    /** Só uma requisição pendente pode ser aprovada. */
    public void aprovar() {
        exigirStatus(StatusRequisicao.PENDENTE, "aprovada");
        this.status = StatusRequisicao.APROVADA;
    }

    /** HU13 — só pedidos pendentes podem ser recusados, e o motivo é obrigatório. */
    public void recusar(String motivo) {
        exigirStatus(StatusRequisicao.PENDENTE, "recusada");
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Motivo da recusa é obrigatório");
        }
        this.status = StatusRequisicao.RECUSADA;
        this.motivoRecusa = motivo.trim();
    }

    public void registrarRota(String descricao, Integer km) {
        this.rotaEntrega = descricao;
        this.distanciaKm = km;
    }

    /** Só uma requisição aprovada (com bolsas reservadas) pode ser atendida. */
    public void atender(String responsavel) {
        exigirStatus(StatusRequisicao.APROVADA, "atendida");
        if (responsavel == null || responsavel.isBlank()) {
            throw new IllegalArgumentException("Responsável pela distribuição é obrigatório");
        }
        this.status = StatusRequisicao.ATENDIDA;
        this.dataAtendimento = LocalDateTime.now();
        this.responsavelDistribuicao = responsavel;
    }

    private void exigirStatus(StatusRequisicao esperado, String acao) {
        if (this.status != esperado) {
            throw new IllegalStateException("Requisição com status '" + status
                    + "' não pode ser " + acao + " (esperado: '" + esperado + "')");
        }
    }

    public boolean isPendente() { return status == StatusRequisicao.PENDENTE; }
    public boolean isAprovada() { return status == StatusRequisicao.APROVADA; }
    public boolean isAtendida() { return status == StatusRequisicao.ATENDIDA; }
    public boolean isRecusada() { return status == StatusRequisicao.RECUSADA; }

    /** Mantido para compatibilidade com as telas e com a FilaRequisicoes. */
    public String getHospitalId() {
        return hospital != null ? hospital.getId() : null;
    }

    // ===================== Getters / Setters =====================

    public Hospital getHospital() { return hospital; }
    public void setHospital(Hospital hospital) { this.hospital = hospital; }

    public String getTipoSanguineo() { return tipoSanguineo; }
    public void setTipoSanguineo(String tipoSanguineo) { this.tipoSanguineo = tipoSanguineo; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public boolean isUrgente() { return urgente; }
    public void setUrgente(boolean urgente) { this.urgente = urgente; }

    public StatusRequisicao getStatus() { return status; }
    public void setStatus(StatusRequisicao status) { this.status = status; }

    public LocalDateTime getDataSolicitacao() { return dataSolicitacao; }
    public void setDataSolicitacao(LocalDateTime dataSolicitacao) { this.dataSolicitacao = dataSolicitacao; }

    public LocalDateTime getDataAtendimento() { return dataAtendimento; }
    public void setDataAtendimento(LocalDateTime dataAtendimento) { this.dataAtendimento = dataAtendimento; }

    public String getRotaEntrega() { return rotaEntrega; }
    public Integer getDistanciaKm() { return distanciaKm; }
    public String getMotivoRecusa() { return motivoRecusa; }

    public String getResponsavelDistribuicao() { return responsavelDistribuicao; }
    public void setResponsavelDistribuicao(String responsavelDistribuicao) { this.responsavelDistribuicao = responsavelDistribuicao; }
}
