package br.com.rotavital.model;

import br.com.rotavital.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Bolsa de sangue: entidade central do domínio. Concentra as transições de
 * estado (disponível → reservada → distribuída / vencida → descartada) e
 * protege as invariantes, impedindo por exemplo distribuir uma bolsa duas vezes.
 */
@Entity
@Table(name = "bolsa_sangue")
public class BolsaSangue {

    public static final int DIAS_ALERTA_USO_PRIORITARIO = 5;

    @Id
    @Column(length = 20)
    private String codigo; // BS-00001

    @Column(length = 3, nullable = false)
    private String tipoSanguineo;

    private String componente;
    private int volume; // ml

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StatusBolsa status = StatusBolsa.DISPONIVEL;

    private LocalDate dataColeta;
    private LocalDate dataValidade;
    private LocalDateTime dataDistribuicao;
    private LocalDateTime dataDescarte;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doador_id")
    private Doador doador;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hospital_destino_id")
    private Hospital hospitalDestino;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requisicao_id")
    private Requisicao requisicao;

    private String responsavelDistribuicao;
    private String responsavelDescarte;
    private boolean usoPrioritario;

    public BolsaSangue() {}

    /** Fábrica: cria a bolsa a partir de uma coleta, calculando a validade pelo componente (HU02). */
    public static BolsaSangue coletar(String codigo, Doador doador, Componente componente,
                                      int volume, LocalDate dataColeta) {
        BolsaSangue b = new BolsaSangue();
        b.codigo = codigo;
        b.doador = doador;
        b.tipoSanguineo = doador.getTipoSanguineo();
        b.componente = componente.getDescricao();
        b.volume = volume;
        b.dataColeta = dataColeta;
        b.dataValidade = dataColeta.plusDays(componente.getDiasValidade());
        b.status = StatusBolsa.DISPONIVEL;
        return b;
    }

    // ===================== Regras de negócio =====================

    public boolean isDisponivel() {
        return status == StatusBolsa.DISPONIVEL;
    }

    public boolean venceuEm(LocalDate data) {
        return !dataValidade.isAfter(data);
    }

    public long diasParaVencer(LocalDate hoje) {
        return ChronoUnit.DAYS.between(hoje, dataValidade);
    }

    public void reservarPara(Requisicao req) {
        if (!isDisponivel()) {
            throw new RegraNegocioException("Bolsa " + codigo + " não está disponível para reserva");
        }
        this.status = StatusBolsa.RESERVADA;
        this.requisicao = req;
    }

    public void distribuir(String responsavel, Hospital destino) {
        if (status == StatusBolsa.DISTRIBUIDA) {
            throw new RegraNegocioException("Bolsa já distribuída anteriormente");
        }
        if (status == StatusBolsa.VENCIDA || status == StatusBolsa.DESCARTADA) {
            throw new RegraNegocioException("Bolsa " + status + " não pode ser distribuída");
        }
        this.status = StatusBolsa.DISTRIBUIDA;
        this.dataDistribuicao = LocalDateTime.now();
        this.responsavelDistribuicao = responsavel;
        if (destino != null) this.hospitalDestino = destino;
        this.usoPrioritario = false;
    }

    public void descartar(String responsavel) {
        if (status == StatusBolsa.DESCARTADA) {
            throw new RegraNegocioException("Bolsa já foi descartada");
        }
        if (status == StatusBolsa.DISTRIBUIDA) {
            throw new RegraNegocioException("Bolsa já distribuída não pode ser descartada");
        }
        this.status = StatusBolsa.DESCARTADA;
        this.dataDescarte = LocalDateTime.now();
        this.responsavelDescarte = responsavel;
        this.usoPrioritario = false;
    }

    /**
     * Verificação diária (HU03 / HU07): marca como vencida ou como uso prioritário.
     * @return true se a bolsa acabou de vencer.
     */
    public boolean atualizarValidade(LocalDate hoje) {
        if (status != StatusBolsa.DISPONIVEL && status != StatusBolsa.RESERVADA) {
            return false;
        }
        if (venceuEm(hoje)) {
            this.status = StatusBolsa.VENCIDA;
            this.usoPrioritario = false;
            return true;
        }
        if (status == StatusBolsa.DISPONIVEL) {
            this.usoPrioritario = diasParaVencer(hoje) <= DIAS_ALERTA_USO_PRIORITARIO;
        }
        return false;
    }

    // ===== Atalhos usados nas telas (Thymeleaf) =====

    public String getDoadorId() { return doador != null ? doador.getId() : null; }
    public String getHospitalDestinoId() { return hospitalDestino != null ? hospitalDestino.getId() : null; }
    public String getRequisicaoId() { return requisicao != null ? requisicao.getId() : null; }

    // ===================== Getters / Setters =====================

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getTipoSanguineo() { return tipoSanguineo; }
    public void setTipoSanguineo(String tipoSanguineo) { this.tipoSanguineo = tipoSanguineo; }

    public String getComponente() { return componente; }
    public void setComponente(String componente) { this.componente = componente; }

    public int getVolume() { return volume; }
    public void setVolume(int volume) { this.volume = volume; }

    public StatusBolsa getStatus() { return status; }
    public void setStatus(StatusBolsa status) { this.status = status; }

    public LocalDate getDataColeta() { return dataColeta; }
    public void setDataColeta(LocalDate dataColeta) { this.dataColeta = dataColeta; }

    public LocalDate getDataValidade() { return dataValidade; }
    public void setDataValidade(LocalDate dataValidade) { this.dataValidade = dataValidade; }

    public LocalDateTime getDataDistribuicao() { return dataDistribuicao; }
    public LocalDateTime getDataDescarte() { return dataDescarte; }

    public Doador getDoador() { return doador; }
    public void setDoador(Doador doador) { this.doador = doador; }

    public Hospital getHospitalDestino() { return hospitalDestino; }
    public Requisicao getRequisicao() { return requisicao; }

    public String getResponsavelDistribuicao() { return responsavelDistribuicao; }
    public String getResponsavelDescarte() { return responsavelDescarte; }

    public boolean isUsoPrioritario() { return usoPrioritario; }
    public void setUsoPrioritario(boolean usoPrioritario) { this.usoPrioritario = usoPrioritario; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BolsaSangue outra)) return false;
        return codigo != null && codigo.equals(outra.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(codigo);
    }
}
