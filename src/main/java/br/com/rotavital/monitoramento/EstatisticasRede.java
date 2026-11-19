package br.com.rotavital.monitoramento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cálculos das métricas de rede usadas no painel e no relatório de benchmarking.
 * Mesmas definições do script docs/rsd/benchmark/benchmark_rede.py, para que os
 * números do painel e do benchmark externo sejam comparáveis.
 */
public final class EstatisticasRede {

    private EstatisticasRede() {
    }

    /** Latências (ms) só das medições bem-sucedidas, na ordem original. */
    public static List<Double> latenciasComSucesso(List<Amostra> amostras) {
        List<Double> lat = new ArrayList<>();
        for (Amostra a : amostras) {
            if (a.sucesso()) {
                lat.add(a.latenciaMs());
            }
        }
        return lat;
    }

    public static double media(List<Double> valores) {
        if (valores.isEmpty()) {
            return 0.0;
        }
        double soma = 0;
        for (double v : valores) {
            soma += v;
        }
        return soma / valores.size();
    }

    /**
     * Percentil pelo método do posto mais próximo (nearest-rank):
     * posição = teto(p/100 · n), contando a partir de 1 na lista ordenada.
     */
    public static double percentil(List<Double> valores, double p) {
        if (valores.isEmpty()) {
            return 0.0;
        }
        if (p <= 0 || p > 100) {
            throw new IllegalArgumentException("Percentil deve estar em (0, 100]");
        }
        List<Double> ordenados = new ArrayList<>(valores);
        Collections.sort(ordenados);
        int posicao = (int) Math.ceil(p / 100.0 * ordenados.size());
        return ordenados.get(Math.max(posicao, 1) - 1);
    }

    /**
     * Jitter = média das variações absolutas entre medições consecutivas,
     * |L(i) − L(i−1)| (variação de atraso entre pacotes, como na RFC 3550, sem a suavização 1/16).
     * Com menos de duas medições não há variação: retorna 0.
     */
    public static double jitter(List<Double> latenciasEmOrdem) {
        if (latenciasEmOrdem.size() < 2) {
            return 0.0;
        }
        double soma = 0;
        for (int i = 1; i < latenciasEmOrdem.size(); i++) {
            soma += Math.abs(latenciasEmOrdem.get(i) - latenciasEmOrdem.get(i - 1));
        }
        return soma / (latenciasEmOrdem.size() - 1);
    }

    /** Perda = medições que falharam ÷ total de medições × 100. */
    public static double perdaPercentual(List<Amostra> amostras) {
        if (amostras.isEmpty()) {
            return 0.0;
        }
        long falhas = amostras.stream().filter(a -> !a.sucesso()).count();
        return 100.0 * falhas / amostras.size();
    }

    /**
     * Vazão em medições (requisições) por segundo dentro da janela.
     * Usa o menor entre a janela pedida e o tempo desde a primeira medição,
     * para não subestimar logo depois que a aplicação sobe.
     */
    public static double vazaoPorSegundo(List<Amostra> amostras, long agoraMs, long janelaMs) {
        if (amostras.isEmpty()) {
            return 0.0;
        }
        long decorrido = agoraMs - amostras.get(0).instanteMs();
        double segundos = Math.max(1.0, Math.min(janelaMs, decorrido) / 1000.0);
        return amostras.size() / segundos;
    }

    public static double arredondar(double valor, int casas) {
        double fator = Math.pow(10, casas);
        return Math.round(valor * fator) / fator;
    }
}
