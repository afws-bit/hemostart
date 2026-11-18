package br.com.rotavital.dto.api;

import java.time.LocalDate;

/** Saída de GET /api/v1/relatorios/doacoes e /distribuicoes. */
public record RelatorioResponse(String relatorio, LocalDate inicio, LocalDate fim, String tipoSanguineo,
                                String hospitalId, long total, long coletadas, long distribuidas,
                                long descartadas) {
}
