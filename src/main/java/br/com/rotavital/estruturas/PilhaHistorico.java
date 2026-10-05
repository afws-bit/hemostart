package br.com.rotavital.estruturas;

import java.util.ArrayList;
import java.util.List;

public class PilhaHistorico<T> {

    private static class No<T> {
        T dado;
        No<T> next;

        No(T dado) {
            this.dado = dado;
            this.next = null;
        }
    }

    private No<T> top;
    private int tamanho;

    public PilhaHistorico() {
        this.top = null;
        this.tamanho = 0;
    }

    public void empilhar(T dado) {
        No<T> novo = new No<>(dado);
        novo.next = top;
        top = novo;
        tamanho++;
    }

    public T desempilhar() {
        if (top == null) return null;
        T dado = top.dado;
        top = top.next;
        tamanho--;
        return dado;
    }

    public T topo() {
        return top != null ? top.dado : null;
    }

    public boolean vazia() {
        return top == null;
    }

    public int tamanho() {
        return tamanho;
    }

    public List<T> paraLista() {
        List<T> lista = new ArrayList<>();
        No<T> atual = top;
        while (atual != null) {
            lista.add(atual.dado);
            atual = atual.next;
        }
        return lista;
    }
}
