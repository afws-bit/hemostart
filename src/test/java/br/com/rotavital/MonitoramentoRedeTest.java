package br.com.rotavital;

import br.com.rotavital.monitoramento.Amostra;
import br.com.rotavital.monitoramento.EstatisticasRede;
import br.com.rotavital.monitoramento.SerieMedicoes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RSD (U2) — cálculos do painel de rede: percentis, jitter, perda, vazão e o buffer circular.
 * Valores esperados calculados à mão.
 */
class MonitoramentoRedeTest {

    private static Amostra ok(long t, double ms) {
        return new Amostra(t, ms, true, "GET /");
    }

    private static Amostra falha(long t) {
        return new Amostra(t, 0, false, "GET /");
    }

    @Test
    void percentilPeloPostoMaisProximo() {
        List<Double> v = List.of(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0);
        assertEquals(50.0, EstatisticasRede.percentil(v, 50), 1e-9);   // teto(0,5·10)=5º
        assertEquals(100.0, EstatisticasRede.percentil(v, 95), 1e-9);  // teto(9,5)=10º
        assertEquals(10.0, EstatisticasRede.percentil(v, 1), 1e-9);    // teto(0,1)=1º
        assertEquals(0.0, EstatisticasRede.percentil(List.of(), 95), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> EstatisticasRede.percentil(v, 0));
    }

    @Test
    void jitterEhAMediaDasVariacoesAbsolutas() {
        // |12-10| + |9-12| + |15-9| = 2 + 3 + 6 = 11 → 11/3
        assertEquals(11.0 / 3, EstatisticasRede.jitter(List.of(10.0, 12.0, 9.0, 15.0)), 1e-9);
        assertEquals(0.0, EstatisticasRede.jitter(List.of(7.0, 7.0, 7.0)), 1e-9);
        assertEquals(0.0, EstatisticasRede.jitter(List.of(42.0)), 1e-9);
    }

    @Test
    void perdaConsideraSoAsFalhas() {
        List<Amostra> a = List.of(ok(1, 10), falha(2), ok(3, 12), ok(4, 11));
        assertEquals(25.0, EstatisticasRede.perdaPercentual(a), 1e-9);
        assertEquals(List.of(10.0, 12.0, 11.0), EstatisticasRede.latenciasComSucesso(a));
        assertEquals(11.0, EstatisticasRede.media(EstatisticasRede.latenciasComSucesso(a)), 1e-9);
        assertEquals(0.0, EstatisticasRede.perdaPercentual(List.of()), 1e-9);
    }

    @Test
    void vazaoUsaOMenorEntreJanelaETempoDecorrido() {
        List<Amostra> a = List.of(ok(0, 5), ok(5_000, 5), ok(10_000, 5));
        // 3 medições em 10 s (decorrido < janela de 60 s) → 0,3/s
        assertEquals(0.3, EstatisticasRede.vazaoPorSegundo(a, 10_000, 60_000), 1e-9);
        // janela de 5 s menor que o decorrido → 3 / 5 = 0,6/s
        assertEquals(0.6, EstatisticasRede.vazaoPorSegundo(a, 10_000, 5_000), 1e-9);
    }

    @Test
    void bufferCircularDescartaAsMaisAntigas() {
        SerieMedicoes s = new SerieMedicoes(3);
        for (int i = 1; i <= 5; i++) {
            s.adicionar(ok(i, i * 10));
        }
        List<Amostra> c = s.copia();
        assertEquals(3, s.tamanho());
        assertEquals(5, s.totalRegistrado());
        assertEquals(List.of(3L, 4L, 5L), c.stream().map(Amostra::instanteMs).toList());
        assertEquals(2, s.desde(4).size());
        assertThrows(IllegalArgumentException.class, () -> new SerieMedicoes(0));
    }
}
