package br.com.rotavital.aed;

import br.com.rotavital.model.BolsaSangue;

/**
 * AED - Unidade 2: índice de estoque por HASH.
 * Tradução de TabelaHash.cpp: vetor de 101 baldes, encadeamento separado,
 * chave = tipo sanguíneo, valor = FilaFEFO das bolsas desse tipo.
 * A função djb2 é a mesma; como Java não tem "unsigned long", o resto da divisão
 * usa Long.remainderUnsigned, o que dá exatamente o mesmo índice que o C++
 * (ex.: hash("O-") = 16, hash("A+") = 57 nas duas versões).
 *   buscar/inserir: O(1) em média; O(k) no pior caso (k = entradas no mesmo balde).
 */
public class TabelaHashEstoque {

    private static final int TAMANHO_TABELA = 101;

    private static final class EntradaHash {
        final String chave;
        final FilaFEFO bolsas = new FilaFEFO();
        EntradaHash proximo;

        EntradaHash(String chave) {
            this.chave = chave;
        }
    }

    private final EntradaHash[] baldes = new EntradaHash[TAMANHO_TABELA];

    /** djb2: hash = hash * 33 + c, começando em 5381; resultado módulo 101. */
    public int funcaoHash(String chave) {
        long hash = 5381;
        for (int i = 0; i < chave.length(); i++) {
            hash = ((hash << 5) + hash) + (chave.charAt(i) & 0xFF);
        }
        return (int) Long.remainderUnsigned(hash, TAMANHO_TABELA);
    }

    /** Fila FEFO do tipo, ou null se o tipo não tem bolsas. */
    public FilaFEFO buscar(String tipo) {
        EntradaHash atual = baldes[funcaoHash(tipo)];
        while (atual != null) {
            if (atual.chave.equals(tipo)) return atual.bolsas;
            atual = atual.proximo;
        }
        return null;
    }

    public void inserir(BolsaSangue bolsa) {
        String tipo = bolsa.getTipoSanguineo();
        FilaFEFO fila = buscar(tipo);
        if (fila == null) {
            int indice = funcaoHash(tipo);
            EntradaHash nova = new EntradaHash(tipo);
            nova.proximo = baldes[indice]; // insere no início do balde
            baldes[indice] = nova;
            fila = nova.bolsas;
        }
        fila.inserir(bolsa);
    }

    public int quantidade(String tipo) {
        FilaFEFO fila = buscar(tipo);
        return fila == null ? 0 : fila.getTamanho();
    }
}
