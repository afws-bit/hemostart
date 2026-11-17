package br.com.rotavital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.Period;

@Entity
@Table(name = "doador")
public class Doador extends EntidadeBase {

    public static final int IDADE_MINIMA = 16;
    public static final int IDADE_MAXIMA = 69;
    public static final int IDADE_MAXIMA_PRIMEIRA_DOACAO = 59;
    public static final double PESO_MINIMO = 50.0;
    public static final int INTERVALO_DIAS_HOMEM = 60;
    public static final int INTERVALO_DIAS_MULHER = 90;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    private LocalDate dataNascimento;

    @Column(length = 1)
    private String sexo; // M ou F

    @Column(length = 3)
    private String tipoSanguineo;

    private double peso;
    private String contato;

    @Column(length = 1000)
    private String historicoSaude;

    private boolean autorizacaoResponsavel;
    private LocalDate ultimaDoacao;

    public Doador() {}

    // ===================== Regras de negócio =====================

    public int idadeEm(LocalDate data) {
        return Period.between(dataNascimento, data).getYears();
    }

    public boolean isPrimeiraDoacao() {
        return ultimaDoacao == null;
    }

    /** Intervalo mínimo entre doações: 60 dias (homens) ou 90 dias (mulheres). */
    public int intervaloMinimoDias() {
        return "F".equalsIgnoreCase(sexo) ? INTERVALO_DIAS_MULHER : INTERVALO_DIAS_HOMEM;
    }

    public boolean aptoPorIntervalo(LocalDate hoje) {
        if (isPrimeiraDoacao()) return true;
        return !hoje.isBefore(ultimaDoacao.plusDays(intervaloMinimoDias()));
    }

    public void registrarDoacao(LocalDate data) {
        this.ultimaDoacao = data;
    }

    public TipoSanguineo getTipo() {
        return TipoSanguineo.deRotulo(tipoSanguineo);
    }

    // ===================== Getters / Setters =====================

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getTipoSanguineo() { return tipoSanguineo; }
    public void setTipoSanguineo(String tipoSanguineo) { this.tipoSanguineo = tipoSanguineo; }

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }

    public String getContato() { return contato; }
    public void setContato(String contato) { this.contato = contato; }

    public String getHistoricoSaude() { return historicoSaude; }
    public void setHistoricoSaude(String historicoSaude) { this.historicoSaude = historicoSaude; }

    public boolean isAutorizacaoResponsavel() { return autorizacaoResponsavel; }
    public void setAutorizacaoResponsavel(boolean autorizacaoResponsavel) { this.autorizacaoResponsavel = autorizacaoResponsavel; }

    public LocalDate getUltimaDoacao() { return ultimaDoacao; }
    public void setUltimaDoacao(LocalDate ultimaDoacao) { this.ultimaDoacao = ultimaDoacao; }
}
