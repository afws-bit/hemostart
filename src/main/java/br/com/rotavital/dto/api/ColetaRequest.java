package br.com.rotavital.dto.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/** Entrada de POST /api/v1/coletas. */
public record ColetaRequest(
        @NotBlank(message = "doadorId é obrigatório") String doadorId,
        @NotBlank(message = "componente é obrigatório") String componente,
        @Positive(message = "volume deve ser maior que zero") int volume) {
}
