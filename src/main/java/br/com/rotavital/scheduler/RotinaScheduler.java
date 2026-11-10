package br.com.rotavital.scheduler;

import br.com.rotavital.service.RotinaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RotinaScheduler {

    private static final Logger log = LoggerFactory.getLogger(RotinaScheduler.class);
    private final RotinaService rotinaService;

    public RotinaScheduler(RotinaService rotinaService) {
        this.rotinaService = rotinaService;
    }

    @Scheduled(fixedRate = 60000)
    public void disparar() {
        log.info("[SO-SCHEDULER] Disparo agendado na thread: {}", Thread.currentThread().getName());
        rotinaService.executar();
    }
}