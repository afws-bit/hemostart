package br.com.rotavital.dto.api;

import jakarta.validation.constraints.NotBlank;

/** Entrada de POST /api/v1/solicitacoes/{id}/recusa. */
public record RecusaRequest(@NotBlank(message = "motivo é obrigatório") String motivo) {
}
