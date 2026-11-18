package br.com.rotavital.service;

import br.com.rotavital.dto.RiscoDesabastecimentoDTO;
import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Configuracao;
import br.com.rotavital.model.StatusBolsa;
import br.com.rotavital.model.TipoSanguineo;
import br.com.rotavital.repository.BolsaSangueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * EST — Unidade 2: probabilidade de desabastecimento por tipo sanguíneo.
 *
 * Modelo (detalhado em docs/est/relatorio-desabastecimento.md):
 *  1. Demanda diária do tipo: λ = bolsas do tipo distribuídas nos últimos J dias ÷ J.
 *  2. Demanda no horizonte de H dias: D ~ Poisson(μ), com μ = λ·H
 *     (pedidos chegam de forma independente, a uma taxa média constante).
 *  3. Estoque útil: S = disponíveis − bolsas que vencem dentro do horizonte
 *     (premissa conservadora: elas podem vencer antes de serem usadas; novas coletas não entram).
 *  4. Folga até o mínimo: k = S − mínimo configurado (HU10).
 *  5. Risco = P(D > k) = 1 − Σ_{i=0..k} e^(−μ)·μ^i / i!   (se k < 0, o risco é 1).
 */
@Service
public class EstatisticaService {

    public static final double LIMITE_MODERADO = 0.20;
    public static final double LIMITE_ALTO = 0.50;

    private final BolsaSangueRepository bolsaRepository;
    private final ConfiguracaoService configuracaoService;

    public EstatisticaService(BolsaSangueRepository bolsaRepository, ConfiguracaoService configuracaoService) {
        this.bolsaRepository = bolsaRepository;
        this.configuracaoService = configuracaoService;
    }

    @Transactional(readOnly = true)
    public List<RiscoDesabastecimentoDTO> riscoDesabastecimento(int horizonteDias, int janelaDias) {
        if (horizonteDias < 1 || janelaDias < 1) {
            throw new IllegalArgumentException("horizonteDias e janelaDias devem ser pelo menos 1");
        }
        LocalDate hoje = LocalDate.now();
        LocalDate inicioJanela = hoje.minusDays(janelaDias);
        LocalDate fimHorizonte = hoje.plusDays(horizonteDias);
        List<BolsaSangue> todas = bolsaRepository.findAll();
        Configuracao config = configuracaoService.obter();

        List<RiscoDesabastecimentoDTO> resultado = new ArrayList<>();
        for (String tipo : TipoSanguineo.rotulos()) {
            long distribuidas = todas.stream()
                    .filter(b -> b.getTipoSanguineo().equals(tipo) && b.getDataDistribuicao() != null)
                    .filter(b -> !b.getDataDistribuicao().toLocalDate().isBefore(inicioJanela))
                    .count();
            long disponiveis = todas.stream()
                    .filter(b -> b.getTipoSanguineo().equals(tipo) && b.getStatus() == StatusBolsa.DISPONIVEL)
                    .count();
            long vencendo = todas.stream()
                    .filter(b -> b.getTipoSanguineo().equals(tipo) && b.getStatus() == StatusBolsa.DISPONIVEL)
                    .filter(b -> !b.getDataValidade().isAfter(fimHorizonte))
                    .count();

            double lambda = (double) distribuidas / janelaDias;
            double mu = lambda * horizonteDias;
            int minimo = config.minimoPara(tipo);
            long folga = disponiveis - vencendo - minimo;
            double prob = probabilidadeDemandaMaiorQue(folga, mu);

            resultado.add(new RiscoDesabastecimentoDTO(tipo, disponiveis, vencendo, minimo,
                    arredondar(lambda), arredondar(mu), arredondar(prob), nivel(prob)));
        }
        return resultado;
    }

    /** P(X > k) para X ~ Poisson(mu). Se k < 0 o estoque útil já está abaixo do mínimo: risco 1. */
    public static double probabilidadeDemandaMaiorQue(long k, double mu) {
        if (k < 0) return 1.0;
        if (mu <= 0) return 0.0;
        double termo = Math.exp(-mu); // P(X = 0)
        double acumulada = termo;
        for (long i = 1; i <= k; i++) {
            termo = termo * mu / i;     // P(X = i) a partir de P(X = i-1)
            acumulada += termo;
        }
        return Math.max(0.0, Math.min(1.0, 1.0 - acumulada));
    }

    public static String nivel(double probabilidade) {
        if (probabilidade > LIMITE_ALTO) return "ALTO";
        if (probabilidade >= LIMITE_MODERADO) return "MODERADO";
        return "BAIXO";
    }

    private static double arredondar(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
