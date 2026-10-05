package br.com.rotavital.scheduler;

import br.com.rotavital.service.EstoqueService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ValidadeScheduler {

    private final EstoqueService estoqueService;

    public ValidadeScheduler(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @Scheduled(fixedDelayString = "${scheduling.interval.ms:60000}")
    public void verificarValidade() {
        estoqueService.verificarValidade();
    }
}
