package br.com.rotavital.monitoramento;

import java.util.ArrayList;
import java.util.List;

/**
 * Buffer circular de tamanho fixo com as últimas medições de uma ligação.
 * Quando enche, a medição mais antiga é sobrescrita, então a memória usada é constante.
 * Os métodos são synchronized porque o filtro HTTP grava a partir de várias threads do Tomcat
 * enquanto o painel lê.
 */
public class SerieMedicoes {

    private final Amostra[] buffer;
    private int proximo = 0;
    private int tamanho = 0;
    private long totalRegistrado = 0;

    public SerieMedicoes(int capacidade) {
        if (capacidade <= 0) {
            throw new IllegalArgumentException("Capacidade deve ser positiva");
        }
        this.buffer = new Amostra[capacidade];
    }

    public synchronized void adicionar(Amostra amostra) {
        buffer[proximo] = amostra;
        proximo = (proximo + 1) % buffer.length;
        if (tamanho < buffer.length) {
            tamanho++;
        }
        totalRegistrado++;
    }

    /** Cópia das medições em ordem cronológica (mais antiga primeiro). */
    public synchronized List<Amostra> copia() {
        List<Amostra> lista = new ArrayList<>(tamanho);
        int inicio = (proximo - tamanho + buffer.length) % buffer.length;
        for (int i = 0; i < tamanho; i++) {
            lista.add(buffer[(inicio + i) % buffer.length]);
        }
        return lista;
    }

    /** Medições cujo instante é maior ou igual a {@code desdeMs}, em ordem cronológica. */
    public List<Amostra> desde(long desdeMs) {
        List<Amostra> todas = copia();
        List<Amostra> filtradas = new ArrayList<>();
        for (Amostra a : todas) {
            if (a.instanteMs() >= desdeMs) {
                filtradas.add(a);
            }
        }
        return filtradas;
    }

    public synchronized int tamanho() {
        return tamanho;
    }

    public synchronized long totalRegistrado() {
        return totalRegistrado;
    }

    public int capacidade() {
        return buffer.length;
    }
}
