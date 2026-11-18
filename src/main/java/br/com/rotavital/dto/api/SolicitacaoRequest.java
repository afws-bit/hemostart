package br.com.rotavital.dto.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Entrada de POST /api/v1/solicitacoes. */
public record SolicitacaoRequest(
        @NotBlank(message = "cnpjHospital é obrigatório") String cnpjHospital,
        @NotBlank @Pattern(regexp = "(A|B|AB|O)[+-]", message = "tipoSanguineo inválido") String tipoSanguineo,
        @Min(value = 1, message = "quantidade deve ser pelo menos 1") int quantidade,
        boolean urgente) {
}
