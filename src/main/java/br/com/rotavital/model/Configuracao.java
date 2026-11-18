package br.com.rotavital.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import java.util.HashMap;
import java.util.Map;

/** Configuração única do sistema: nível mínimo de estoque por tipo sanguíneo (HU10). */
@Entity
@Table(name = "configuracao")
public class Configuracao {

    public static final long ID_UNICO = 1L;
    public static final int MINIMO_PADRAO = 5;
    public static final int LIMITE_VAGAS_PADRAO = 2;

    @Id
    private Long id = ID_UNICO;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "nivel_minimo_estoque", joinColumns = @JoinColumn(name = "configuracao_id"))
    @MapKeyColumn(name = "tipo_sanguineo", length = 3)
    @Column(name = "minimo")
    private Map<String, Integer> minimoEstoque = new HashMap<>();

    /** HU11 — quantos doadores podem ser agendados no mesmo dia e horário. */
    private Integer limiteVagasPorHorario = LIMITE_VAGAS_PADRAO;

    public Configuracao() {
        for (String tipo : TipoSanguineo.rotulos()) {
            minimoEstoque.put(tipo, MINIMO_PADRAO);
        }
    }

    public int minimoPara(String tipo) {
        return minimoEstoque.getOrDefault(tipo, 0);
    }

    public void definirMinimo(String tipo, int minimo) {
        if (minimo < 0) {
            throw new IllegalArgumentException("O nível mínimo não pode ser negativo");
        }
        minimoEstoque.put(tipo, minimo);
    }

    /** Estoque crítico: disponíveis abaixo do mínimo configurado. */
    public boolean estoqueCritico(String tipo, long disponiveis) {
        return disponiveis < minimoPara(tipo);
    }

    public int getLimiteVagasPorHorario() {
        return limiteVagasPorHorario == null ? LIMITE_VAGAS_PADRAO : limiteVagasPorHorario;
    }

    public void setLimiteVagasPorHorario(int limite) {
        if (limite < 1) throw new IllegalArgumentException("O limite de vagas deve ser pelo menos 1");
        this.limiteVagasPorHorario = limite;
    }

    public Long getId() { return id; }
    public Map<String, Integer> getMinimoEstoque() { return minimoEstoque; }
    public void setMinimoEstoque(Map<String, Integer> minimoEstoque) { this.minimoEstoque = minimoEstoque; }
}
