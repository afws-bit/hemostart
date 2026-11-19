package br.com.rotavital.observabilidade;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Cria um span (Micrometer Observation) para cada chamada às camadas de serviço, de seção crítica
 * e de repositório. Junto com o span HTTP criado pelo Spring, um trace no Tempo fica assim:
 *
 * <pre>
 * http post /api/v1/coletas
 *   └─ EstoqueService.registrarColeta          (camada = servico)
 *        └─ SecaoCritica.executar               (camada = servico)
 *             ├─ DoadorRepository.findById      (camada = banco)
 *             └─ BolsaSangueRepository.save     (camada = banco)
 * </pre>
 *
 * Cada observação também vira métrica (timers {@code hemostart_servico_seconds} e
 * {@code hemostart_repositorio_seconds}, com as tags camada, classe e metodo).
 * Fica por fora das transações ({@code @Order(1)}), então o span inclui o commit.
 */
@Aspect
@Component
@Order(1)
public class ObservabilidadeAspect {

    private final ObservationRegistry registry;

    public ObservabilidadeAspect(ObjectProvider<ObservationRegistry> registry) {
        this.registry = registry.getIfAvailable(() -> ObservationRegistry.NOOP);
    }

    @Around("execution(public * br.com.rotavital.service..*.*(..)) "
            + "|| execution(public * br.com.rotavital.concorrencia..*.*(..))")
    public Object servico(ProceedingJoinPoint pjp) throws Throwable {
        Object alvo = pjp.getTarget();
        String classe = alvo != null ? alvo.getClass().getSimpleName()
                : pjp.getSignature().getDeclaringType().getSimpleName();
        return observar("hemostart.servico", "servico", classe, pjp);
    }

    @Around("bean(*Repository)")
    public Object repositorio(ProceedingJoinPoint pjp) throws Throwable {
        return observar("hemostart.repositorio", "banco", nomeDoRepositorio(pjp), pjp);
    }

    private Object observar(String nome, String camada, String classe, ProceedingJoinPoint pjp) throws Throwable {
        String metodo = pjp.getSignature().getName();
        Observation observacao = Observation.createNotStarted(nome, registry)
                .contextualName(classe + "." + metodo)
                .lowCardinalityKeyValue("camada", camada)
                .lowCardinalityKeyValue("classe", classe)
                .lowCardinalityKeyValue("metodo", metodo)
                .start();
        try (Observation.Scope escopo = observacao.openScope()) {
            return pjp.proceed();
        } catch (Throwable erro) {
            observacao.error(erro);
            throw erro;
        } finally {
            observacao.stop();
        }
    }

    /** Repositórios Spring Data são proxies: o nome útil é o da interface (ex.: DoadorRepository). */
    static String nomeDoRepositorio(ProceedingJoinPoint pjp) {
        Object alvo = pjp.getTarget();
        if (alvo != null) {
            try {
                for (Class<?> interfaceDoUsuario : AopProxyUtils.proxiedUserInterfaces(alvo)) {
                    if (interfaceDoUsuario.getName().startsWith("br.com.rotavital")) {
                        return interfaceDoUsuario.getSimpleName();
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException naoEhProxy) {
                // segue para o nome declarado
            }
            for (Class<?> i : alvo.getClass().getInterfaces()) {
                if (i.getName().startsWith("br.com.rotavital")) {
                    return i.getSimpleName();
                }
            }
        }
        return pjp.getSignature().getDeclaringType().getSimpleName();
    }
}
