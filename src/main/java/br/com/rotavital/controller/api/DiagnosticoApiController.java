package br.com.rotavital.controller.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de teste da observabilidade: gera um HTTP 500 de propósito, para que a demonstração
 * mostre o caso de erro no painel de taxa de 5xx (Prometheus), no log ERROR com stack trace (Loki)
 * e no span marcado com erro (Tempo).
 *
 * <p>Só existe quando {@code observabilidade.endpoints-teste.habilitado=true}, o que acontece apenas
 * no ambiente de observabilidade (docker-compose). Em produção (Render) a rota não existe.</p>
 */
@RestController
@RequestMapping("/api/teste")
@ConditionalOnProperty(name = "observabilidade.endpoints-teste.habilitado", havingValue = "true")
public class DiagnosticoApiController {

    @GetMapping("/erro")
    public void erro() {
        throw new IllegalStateException("Erro simulado para a demonstração de observabilidade");
    }
}
