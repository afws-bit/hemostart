package br.com.rotavital.dto.api;

import java.time.LocalDateTime;
import java.util.List;

/** Corpo padronizado de todo erro da API (400, 403, 404, 409, 500). */
public record ErroResponse(LocalDateTime timestamp, int status, String erro, String mensagem,
                           String caminho, List<String> detalhes) {
}
