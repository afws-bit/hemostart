package br.com.rotavital.concorrencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * SO — exclusão mútua nas operações que disputam o mesmo estoque
 * (registrar coleta, aprovar/recusar solicitação, distribuir, descartar,
 * verificar validade e agendar).
 *
 * Por que não basta {@code synchronized} + {@code @Transactional}?
 * O proxy do Spring abre a transação ANTES de entrar no método e só faz o
 * commit DEPOIS de sair dele, ou seja, depois de liberar o lock. Nesse
 * intervalo outra thread entra, lê o banco sem ver o que ainda não foi
 * gravado e pode, por exemplo, gerar o mesmo código de bolsa ou reservar a
 * mesma bolsa para dois pedidos.
 *
 * Aqui a ordem é: pega o lock → abre a transação → executa → COMMIT → solta o lock.
 * O lock é justo (fair = true): as threads entram na ordem em que chegaram.
 * Se a thread já estiver dentro de uma transação (ex.: testes com @Transactional),
 * a operação participa dela (propagação REQUIRED).
 */
@Component
public class SecaoCritica {

    private final ReentrantLock trava = new ReentrantLock(true);
    private final TransactionTemplate transacao;

    public SecaoCritica(PlatformTransactionManager transactionManager) {
        this.transacao = new TransactionTemplate(transactionManager);
    }

    public <T> T executar(Supplier<T> operacao) {
        trava.lock();
        try {
            return transacao.execute(status -> operacao.get());
        } finally {
            trava.unlock();
        }
    }

    public void executar(Runnable operacao) {
        executar(() -> {
            operacao.run();
            return null;
        });
    }
}
