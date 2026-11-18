package br.com.rotavital.dto.api;

import jakarta.validation.constraints.NotBlank;

/** Entrada de POST /api/v1/bolsas/{id}/descarte. */
public record DescarteRequest(@NotBlank(message = "responsavel é obrigatório") String responsavel) {
}
