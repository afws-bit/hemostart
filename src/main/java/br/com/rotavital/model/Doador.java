package br.com.rotavital.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public class Doador {

    private String id;
    private String nome;
    private String cpf;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataNascimento;

    private String sexo; // M ou F
    private String tipoSanguineo;
    private double peso;
    private String contato;
    private String historicoSaude;
    private boolean autorizacaoResponsavel;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate ultimaDoacao;

    public Doador() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
