package br.com.rotavital.dto.api;

import java.util.Map;

/** Uma linha de GET /api/v1/estoque: quantidade por status de um tipo sanguíneo. */
public record EstoqueTipoResponse(String tipoSanguineo, Map<String, Long> quantidadePorStatus,
                                  int nivelMinimo, boolean estoqueCritico) {
}
