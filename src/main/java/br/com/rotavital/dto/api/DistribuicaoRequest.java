package br.com.rotavital.dto.api;

import jakarta.validation.constraints.NotBlank;

/** Entrada de POST /api/v1/distribuicoes. */
public record DistribuicaoRequest(
        @NotBlank(message = "solicitacaoId é obrigatório") String solicitacaoId,
        @NotBlank(message = "responsavel é obrigatório") String responsavel) {
}
