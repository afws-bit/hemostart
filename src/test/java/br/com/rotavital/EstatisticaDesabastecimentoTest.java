package br.com.rotavital;

import br.com.rotavital.dto.RiscoDesabastecimentoDTO;
import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Componente;
import br.com.rotavital.model.Doador;
import br.com.rotavital.repository.BolsaSangueRepository;
import br.com.rotavital.repository.DoadorRepository;
import br.com.rotavital.service.ConfiguracaoService;
import br.com.rotavital.service.EstatisticaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** EST (PI3-84) — probabilidade de desabastecimento por tipo sanguíneo. */
@SpringBootTest
@Transactional
class EstatisticaDesabastecimentoTest {

    @Autowired private EstatisticaService estatisticaService;
    @Autowired private ConfiguracaoService configuracaoService;
    @Autowired private DoadorRepository doadorRepository;
    @Autowired private BolsaSangueRepository bolsaRepository;

    private int seq = 0;

    // ---------- fórmula (sem banco) ----------

    @Test
    void poisson_valoresConhecidos() {
        // X ~ Poisson(2): P(X > 2) = 1 - e^-2 (1 + 2 + 2) = 0.3233
        assertEquals(0.3233, EstatisticaService.probabilidadeDemandaMaiorQue(2, 2.0), 1e-4);
        // P(X > 0) = 1 - e^-mu
        assertEquals(1 - Math.exp(-1.5), EstatisticaService.probabilidadeDemandaMaiorQue(0, 1.5), 1e-9);
    }

    @Test
    void poisson_casosLimite() {
        assertEquals(1.0, EstatisticaService.probabilidadeDemandaMaiorQue(-1, 3.0), "abaixo do mínimo: risco 1");
        assertEquals(0.0, EstatisticaService.probabilidadeDemandaMaiorQue(5, 0.0), "sem demanda: risco 0");
        assertTrue(EstatisticaService.probabilidadeDemandaMaiorQue(30, 2.0) < 1e-9, "folga grande: risco ~0");
    }

    @Test
    void classificacaoDoNivel() {
        assertEquals("BAIXO", EstatisticaService.nivel(0.05));
        assertEquals("MODERADO", EstatisticaService.nivel(0.20));
        assertEquals("ALTO", EstatisticaService.nivel(0.51));
    }

    // ---------- com dados no banco ----------

    private Doador doador(String tipo) {
        Doador d = new Doador();
        seq++;
        d.setNome("Doador " + seq);
        d.setCpf(String.format("6%010d", seq));
        d.setDataNascimento(LocalDate.now().minusYears(30));
        d.setSexo("M");
        d.setTipoSanguineo(tipo);
        d.setPeso(80);
        return doadorRepository.save(d);
    }

    private BolsaSangue bolsa(Doador d, LocalDate coleta) {
        return bolsaRepository.save(BolsaSangue.coletar(String.format("BS-8%04d", ++seq), d,
                Componente.CONCENTRADO_HEMACIAS, 450, coleta));
    }

    private RiscoDesabastecimentoDTO linha(List<RiscoDesabastecimentoDTO> r, String tipo) {
        return r.stream().filter(x -> x.tipoSanguineo().equals(tipo)).findFirst().orElseThrow();
    }

    @Test
    void riscoPorTipoComDemandaEstoqueEVencimento() {
        configuracaoService.definirMinimo("O-", 2);
        configuracaoService.definirMinimo("A+", 2);
        Doador o = doador("O-");
        Doador a = doador("A+");

        // O-: 30 bolsas distribuídas nos últimos 30 dias -> 1 por dia; 3 disponíveis
        for (int i = 0; i < 30; i++) {
            BolsaSangue b = bolsa(o, LocalDate.now().minusDays(5));
            b.distribuir("teste", null);
            bolsaRepository.save(b);
        }
        for (int i = 0; i < 3; i++) bolsa(o, LocalDate.now());

        // A+: nenhuma demanda; 5 disponíveis, 4 delas vencendo em 2 dias
        bolsa(a, LocalDate.now());
        for (int i = 0; i < 4; i++) bolsa(a, LocalDate.now().minusDays(40)); // 42 - 40 = vence em 2 dias

        List<RiscoDesabastecimentoDTO> r = estatisticaService.riscoDesabastecimento(7, 30);
        assertEquals(8, r.size());

        RiscoDesabastecimentoDTO oNeg = linha(r, "O-");
        assertEquals(3, oNeg.disponiveis());
        assertEquals(1.0, oNeg.demandaMediaDiaria(), 1e-9);
        assertEquals(7.0, oNeg.demandaEsperadaNoHorizonte(), 1e-9);
        // folga = 3 - 0 - 2 = 1 -> P(Poisson(7) > 1) = 1 - e^-7 (1 + 7) = 0.9927
        assertEquals(0.9927, oNeg.probabilidade(), 1e-4);
        assertEquals("ALTO", oNeg.nivel());

        RiscoDesabastecimentoDTO aPos = linha(r, "A+");
        assertEquals(4, aPos.vencendoNoHorizonte());
        // estoque útil 5 - 4 = 1 < mínimo 2 -> risco 1 mesmo sem demanda
        assertEquals(1.0, aPos.probabilidade(), 1e-9);

        RiscoDesabastecimentoDTO bPos = linha(r, "B+");
        assertEquals(0, bPos.disponiveis());
        assertEquals("ALTO", bPos.nivel(), "sem bolsas e mínimo padrão 5: já está abaixo do mínimo");
    }
}
