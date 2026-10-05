package br.com.rotavital.model;

public class Hospital {

    private String id;
    private String razaoSocial;
    private String cnpj;
    private String endereco;
    private String contato;
    private boolean ativo;

    public Hospital() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
