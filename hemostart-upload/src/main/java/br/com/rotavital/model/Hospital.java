package br.com.rotavital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "hospital")
public class Hospital extends EntidadeBase {

    @Column(nullable = false)
    private String razaoSocial;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    private String endereco;
    private String contato;
    private boolean ativo = true;

    public Hospital() {}

    /** Somente hospitais ativos podem solicitar sangue (HU05 / HU09). */
    public boolean podeSolicitar() {
        return ativo;
    }

    public void alternarStatus() {
        this.ativo = !this.ativo;
    }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getContato() { return contato; }
    public void setContato(String contato) { this.contato = contato; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
