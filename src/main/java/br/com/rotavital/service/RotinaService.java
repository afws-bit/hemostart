package br.com.rotavital.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;

@Service
public class RotinaService {

    private static final Logger log = LoggerFactory.getLogger(RotinaService.class);
    private final Semaphore semaforo = new Semaphore(1);

    @Async("soTaskExecutor")
    public void executar() {
        if (!semaforo.tryAcquire()) {
            log.warn("[SO-SCHEDULER] Rotina anterior ainda em execução, ignorando. Thread: {}",
                    Thread.currentThread().getName());
            return;
        }
        try {
            log.info("[SO-SCHEDULER] Rotina iniciada na thread: {}", Thread.currentThread().getName());
            Thread.sleep(10000);
            log.info("[SO-SCHEDULER] Rotina finalizada na thread: {}", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaforo.release();
        }
    }
}