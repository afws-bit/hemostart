package br.com.rotavital.notificacao;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Dispara a verificação do estoque logo depois de cada operação que muda a quantidade de bolsas
 * disponíveis ou o mínimo configurado. Fica por fora da transação ({@code @Order(2)}; o
 * {@code @Transactional} tem a menor precedência), então a verificação já enxerga o commit.
 * Uma falha aqui nunca desfaz nem interrompe a operação do usuário.
 */
@Aspect
@Component
@Order(2)
public class NotificacaoEstoqueAspect {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoEstoqueAspect.class);

    private final NotificadorEstoque notificador;

    public NotificacaoEstoqueAspect(NotificadorEstoque notificador) {
        this.notificador = notificador;
    }

    @AfterReturning("execution(public * br.com.rotavital.service.EstoqueService.registrarColeta(..))"
            + " || execution(public * br.com.rotavital.service.EstoqueService.descartar(..))"
            + " || execution(public * br.com.rotavital.service.EstoqueService.verificarValidade(..))"
            + " || execution(public * br.com.rotavital.service.RequisicaoService.aprovarRequisicao(..))"
            + " || execution(public * br.com.rotavital.service.RequisicaoService.reservarBolsasParaPrimeiro(..))"
            + " || execution(public * br.com.rotavital.service.RequisicaoService.confirmarDistribuicao(..))"
            + " || execution(public * br.com.rotavital.service.RequisicaoService.distribuirPorCodigo(..))"
            + " || execution(public * br.com.rotavital.service.ConfiguracaoService.salvarNiveis(..))"
            + " || execution(public * br.com.rotavital.service.ConfiguracaoService.definirMinimo(..))")
    public void aposAlterarEstoque() {
        try {
            notificador.verificarEPublicar();
        } catch (RuntimeException e) {
            log.warn("[WS] Não foi possível notificar a mudança de estoque: {}", e.getMessage());
        }
    }
}
