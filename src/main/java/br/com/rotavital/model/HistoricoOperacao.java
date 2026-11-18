package br.com.rotavital.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Registro imutável de auditoria: cada operação feita sobre uma bolsa. */
@Entity
@Table(name = "historico_operacao")
public class HistoricoOperacao extends EntidadeBase {

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TipoOperacao tipo;

    @Column(length = 20)
    private String codigoBolsa;

    @Column(length = 500)
    private String descricao;

    private LocalDateTime dataHora;

    protected HistoricoOperacao() {}

    public HistoricoOperacao(TipoOperacao tipo, String codigoBolsa, String descricao) {
        this.tipo = tipo;
        this.codigoBolsa = codigoBolsa;
        this.descricao = descricao;
        this.dataHora = LocalDateTime.now();
    }

    public TipoOperacao getTipo() { return tipo; }
    public String getCodigoBolsa() { return codigoBolsa; }
    public String getDescricao() { return descricao; }
    public LocalDateTime getDataHora() { return dataHora; }
}
