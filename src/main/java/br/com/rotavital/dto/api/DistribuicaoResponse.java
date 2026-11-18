package br.com.rotavital.dto.api;

import java.util.List;

/** Saída de POST /api/v1/distribuicoes. */
public record DistribuicaoResponse(SolicitacaoResponse solicitacao, List<BolsaResponse> bolsas) {
}
