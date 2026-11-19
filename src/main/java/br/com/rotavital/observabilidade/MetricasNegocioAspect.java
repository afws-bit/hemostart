package br.com.rotavital.observabilidade;

import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Requisicao;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Liga os eventos de negócio dos serviços às métricas de {@link MetricasNegocio} e escreve um log
 * por evento. Como roda dentro do span do serviço ({@code @Order(2)}, depois do
 * {@link ObservabilidadeAspect}), cada log sai com o traceId e o spanId corretos e o Grafana consegue
 * navegar do log (Loki) para o trace (Tempo).
 *
 * <p>Os serviços não foram alterados: a instrumentação fica toda aqui.</p>
 */
@Aspect
@Component
@Order(2)
public class MetricasNegocioAspect {

    private static final Logger log = LoggerFactory.getLogger(MetricasNegocioAspect.class);

    private final MetricasNegocio metricas;

    public MetricasNegocioAspect(MetricasNegocio metricas) {
        this.metricas = metricas;
    }

    @AfterReturning(pointcut = "execution(* br.com.rotavital.service.EstoqueService.registrarColeta(..))",
            returning = "bolsa")
    public void coleta(BolsaSangue bolsa) {
        if (bolsa != null) {
            metricas.bolsaColetada(bolsa.getTipoSanguineo(), bolsa.getComponente());
            log.info("Coleta registrada: bolsa {} tipo {} componente {}",
                    bolsa.getCodigo(), bolsa.getTipoSanguineo(), bolsa.getComponente());
        }
    }

    @AfterReturning(pointcut = "execution(* br.com.rotavital.service.EstoqueService.descartar(..))",
            returning = "bolsa")
    public void descarte(BolsaSangue bolsa) {
        if (bolsa != null) {
            metricas.bolsaDescartada(bolsa.getTipoSanguineo());
            log.info("Bolsa descartada: {} tipo {}", bolsa.getCodigo(), bolsa.getTipoSanguineo());
        }
    }

    @AfterReturning(pointcut = "execution(* br.com.rotavital.service.RequisicaoService.criarRequisicao(..))",
            returning = "requisicao")
    public void solicitacao(Requisicao requisicao) {
        if (requisicao != null) {
            metricas.solicitacaoCriada(requisicao.getTipoSanguineo(), requisicao.isUrgente());
            log.info("Solicitação recebida: {} tipo {} quantidade {} {}", requisicao.getId(),
                    requisicao.getTipoSanguineo(), requisicao.getQuantidade(),
                    requisicao.isUrgente() ? "URGENTE" : "normal");
        }
    }

    /** Operação crítica: aprovar e reservar bolsas. Mede o tempo e separa sucesso de falha. */
    @Around("execution(* br.com.rotavital.service.RequisicaoService.aprovarRequisicao(..))")
    public Object aprovacao(ProceedingJoinPoint pjp) throws Throwable {
        Timer.Sample amostra = Timer.start();
        try {
            Object resultado = pjp.proceed();
            amostra.stop(metricas.timerAlocacao("aprovada"));
            metricas.solicitacaoDecidida("aprovada");
            if (resultado instanceof Requisicao r) {
                log.info("Solicitação {} aprovada: bolsas reservadas, rota {}", r.getId(), r.getRotaEntrega());
            }
            return resultado;
        } catch (Throwable erro) {
            amostra.stop(metricas.timerAlocacao("falha"));
            throw erro;
        }
    }

    @AfterReturning(pointcut = "execution(* br.com.rotavital.service.RequisicaoService.recusarRequisicao(..))",
            returning = "requisicao")
    public void recusa(Requisicao requisicao) {
        metricas.solicitacaoDecidida("recusada");
        if (requisicao != null) {
            log.info("Solicitação {} recusada: {}", requisicao.getId(), requisicao.getMotivoRecusa());
        }
    }

    @AfterReturning(pointcut = "execution(* br.com.rotavital.service.RequisicaoService.confirmarDistribuicao(..))",
            returning = "bolsas")
    public void distribuicao(List<BolsaSangue> bolsas) {
        if (bolsas != null && !bolsas.isEmpty()) {
            metricas.bolsasDistribuidas(bolsas.get(0).getTipoSanguineo(), bolsas.size());
            log.info("Distribuição confirmada: {} bolsa(s) {}", bolsas.size(), bolsas.get(0).getTipoSanguineo());
        }
    }

    /** Regras de negócio violadas (409, 403, 404) viram log WARN com o traceId da requisição. */
    @AfterThrowing(pointcut = "execution(public * br.com.rotavital.service..*.*(..))", throwing = "erro")
    public void regraVioladaNoServico(JoinPoint jp, RegraNegocioException erro) {
        log.warn("Regra de negócio em {}.{}: {}", jp.getSignature().getDeclaringType().getSimpleName(),
                jp.getSignature().getName(), erro.getMessage());
    }
}
