package br.com.rotavital.aed;

import br.com.rotavital.model.BolsaSangue;

import java.util.ArrayList;
import java.util.List;

/**
 * AED - Unidade 2: priorização FEFO (First Expire, First Out).
 * Tradução de FilaFEFO.cpp: lista simplesmente encadeada mantida ORDENADA por validade.
 * O struct BolsaFEFO* vira a classe interna No; o ponteiro "proximo" vira a referência "proximo";
 * new/delete viram "new No(...)" e a perda da última referência (o coletor de lixo libera o nó).
 *   inserir: O(n) | proximaAVencer / retirarProximaAVencer: O(1)
 */
public class FilaFEFO {

    private static final class No {
        final BolsaSangue bolsa;
        No proximo;

        No(BolsaSangue bolsa) {
            this.bolsa = bolsa;
        }
    }

    private No inicio;
    private int tamanho;

    public void inserir(BolsaSangue bolsa) {
        No nova = new No(bolsa);
        tamanho++;

        // Caso 1: lista vazia ou a nova vence antes da primeira -> vira o início
        if (inicio == null || bolsa.getDataValidade().isBefore(inicio.bolsa.getDataValidade())) {
            nova.proximo = inicio;
            inicio = nova;
            return;
        }

        // Caso 2: anda até o último nó com validade <= nova (empates mantêm a ordem de chegada)
        No atual = inicio;
        while (atual.proximo != null && !atual.proximo.bolsa.getDataValidade().isAfter(bolsa.getDataValidade())) {
            atual = atual.proximo;
        }
        nova.proximo = atual.proximo;
        atual.proximo = nova;
    }

    public boolean vazia() { return inicio == null; }

    public int getTamanho() { return tamanho; }

    /** Bolsa mais urgente sem remover (null se vazia). */
    public BolsaSangue proximaAVencer() {
        return inicio == null ? null : inicio.bolsa;
    }

    /** Remove e devolve a bolsa mais urgente (null se vazia). */
    public BolsaSangue retirarProximaAVencer() {
        if (inicio == null) return null;
        BolsaSangue b = inicio.bolsa;
        inicio = inicio.proximo; // o nó antigo fica sem referência e é coletado (equivale ao delete)
        tamanho--;
        return b;
    }

    public List<BolsaSangue> paraLista() {
        List<BolsaSangue> lista = new ArrayList<>();
        for (No atual = inicio; atual != null; atual = atual.proximo) {
            lista.add(atual.bolsa);
        }
        return lista;
    }
}
