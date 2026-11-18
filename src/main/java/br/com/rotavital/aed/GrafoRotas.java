package br.com.rotavital.aed;

import java.util.ArrayList;
import java.util.List;

/**
 * AED - Unidade 2: roteirização pelo VIZINHO MAIS PRÓXIMO.
 * Tradução de Grafo.cpp: grafo não direcionado em matriz de adjacência (km),
 * com 0 = sem estrada direta. A cada passo vai ao destino se ele é vizinho direto;
 * senão, ao vizinho NÃO visitado mais próximo. Heurística gulosa (não garante o ótimo).
 *   rotaVizinhoMaisProximo: O(V²) — até V passos, cada um varre uma linha de V colunas.
 */
public class GrafoRotas {

    public static final int MAX_LOCAIS = 20;

    private final String[] nomes = new String[MAX_LOCAIS];
    private final int[][] distancia = new int[MAX_LOCAIS][MAX_LOCAIS];
    private int qtdLocais;

    private int indice(String nome) {
        for (int i = 0; i < qtdLocais; i++) {
            if (nomes[i].equals(nome)) return i;
        }
        return -1;
    }

    public int adicionarVertice(String nomeLocal) {
        int i = indice(nomeLocal);
        if (i >= 0) return i;
        if (qtdLocais == MAX_LOCAIS) return -1;
        nomes[qtdLocais] = nomeLocal;
        return qtdLocais++;
    }

    public void adicionarRota(String origem, String destino, int km) {
        int a = adicionarVertice(origem);
        int b = adicionarVertice(destino);
        if (a < 0 || b < 0) return;
        distancia[a][b] = km;
        distancia[b][a] = km; // não direcionado
    }

    public Rota rotaVizinhoMaisProximo(String origem, String destino) {
        int atual = indice(origem);
        int alvo = indice(destino);
        if (atual < 0 || alvo < 0) return Rota.naoEncontrada();

        boolean[] visitado = new boolean[MAX_LOCAIS];
        visitado[atual] = true;
        List<String> paradas = new ArrayList<>();
        paradas.add(nomes[atual]);
        int total = 0;

        while (atual != alvo) {
            int proximo = -1;
            if (distancia[atual][alvo] > 0) {
                proximo = alvo;                         // destino é vizinho direto
            } else {
                int menor = 0;
                for (int j = 0; j < qtdLocais; j++) {   // vizinho não visitado mais próximo
                    if (distancia[atual][j] > 0 && !visitado[j]
                            && (proximo == -1 || distancia[atual][j] < menor)) {
                        proximo = j;
                        menor = distancia[atual][j];
                    }
                }
            }
            if (proximo == -1) return Rota.naoEncontrada(); // beco sem saída
            total += distancia[atual][proximo];
            visitado[proximo] = true;
            atual = proximo;
            paradas.add(nomes[atual]);
        }
        return new Rota(List.copyOf(paradas), total, true);
    }

    public boolean contem(String nome) {
        return indice(nome) >= 0;
    }
}
