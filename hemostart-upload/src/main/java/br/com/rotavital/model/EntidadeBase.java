package br.com.rotavital.model;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.util.Objects;
import java.util.UUID;

/**
 * Superclasse das entidades identificadas por UUID (herança + reuso).
 * Gera o identificador antes de persistir e implementa equals/hashCode por id.
 */
@MappedSuperclass
public abstract class EntidadeBase {

    @Id
    private String id;

    @PrePersist
    protected void gerarIdSeNecessario() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntidadeBase outra = (EntidadeBase) o;
        return id != null && id.equals(outra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
