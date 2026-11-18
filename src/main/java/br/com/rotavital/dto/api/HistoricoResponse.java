package br.com.rotavital.dto.api;

import br.com.rotavital.model.HistoricoOperacao;

import java.time.LocalDateTime;

/** Um evento do rastreio de uma bolsa. */
public record HistoricoResponse(String tipo, String descricao, LocalDateTime dataHora) {

    public static HistoricoResponse de(HistoricoOperacao h) {
        return new HistoricoResponse(h.getTipo().name(), h.getDescricao(), h.getDataHora());
    }
}
