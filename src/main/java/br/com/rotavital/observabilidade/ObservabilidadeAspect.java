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

    /**
     * Marca quando a thread está dentro do start()/stop() de uma observação. Os handlers do
     * Micrometer (métricas, tracing) podem acabar chamando beans interceptados por este aspecto;
     * sem a trava isso vira recursão infinita (StackOverflowError).
     */
    private static final ThreadLocal<Boolean> DENTRO_DO_HANDLER = ThreadLocal.withInitial(() -> false);

    private final ObjectProvider<ObservationRegistry> provedorRegistry;
    private volatile ObservationRegistry registry;

    /** O registry é resolvido só no primeiro uso, para não criar referência circular na subida. */
    public ObservabilidadeAspect(ObjectProvider<ObservationRegistry> provedorRegistry) {
        this.provedorRegistry = provedorRegistry;
    }

    private ObservationRegistry registry() {
        ObservationRegistry atual = registry;
        if (atual == null) {
            atual = provedorRegistry.getIfAvailable(() -> ObservationRegistry.NOOP);
            registry = atual;
        }
        return atual;
    }

    @Around("execution(public * br.com.rotavital.service..*.*(..)) "
            + "|| execution(public * br.com.rotavital.concorrencia..*.*(..))")
    public Object servico(ProceedingJoinPoint pjp) throws Throwable {
        Object alvo = pjp.getTarget();
        String classe = alvo != null ? alvo.getClass().getSimpleName()
                : pjp.getSignature().getDeclaringType().getSimpleName();
        return observar("hemostart.servico", "servico", classe, pjp);
    }

    @Around("bean(*Repository) && this(org.springframework.data.repository.Repository)")
    public Object repositorio(ProceedingJoinPoint pjp) throws Throwable {
        return observar("hemostart.repositorio", "banco", nomeDoRepositorio(pjp), pjp);
    }

    private Object observar(String nome, String camada, String classe, ProceedingJoinPoint pjp) throws Throwable {
        if (DENTRO_DO_HANDLER.get()) {
            return pjp.proceed();
        }
        String metodo = pjp.getSignature().getName();
        Observation observacao = Observation.createNotStarted(nome, registry())
                .contextualName(classe + "." + metodo)
                .lowCardinalityKeyValue("camada", camada)
                .lowCardinalityKeyValue("classe", classe)
                .lowCardinalityKeyValue("metodo", metodo);
        emHandler(observacao::start);
        try (Observation.Scope escopo = observacao.openScope()) {
            return pjp.proceed();
        } catch (Throwable erro) {
            observacao.error(erro);
            throw erro;
        } finally {
            emHandler(observacao::stop);
        }
    }

    private static void emHandler(Runnable acao) {
        DENTRO_DO_HANDLER.set(true);
        try {
            acao.run();
        } finally {
            DENTRO_DO_HANDLER.set(false);
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
