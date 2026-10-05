package br.com.rotavital.estruturas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class ListaEncadeada<T> {

    private static class No<T> {
        T dado;
        No<T> next;

        No(T dado) {
            this.dado = dado;
            this.next = null;
        }
    }

    private No<T> head;
    private int tamanho;

    public ListaEncadeada() {
        this.head = null;
        this.tamanho = 0;
    }

    public void inserirInicio(T dado) {
        No<T> novo = new No<>(dado);
        novo.next = head;
        head = novo;
        tamanho++;
    }

    public void inserirFim(T dado) {
        No<T> novo = new No<>(dado);
        if (head == null) {
            head = novo;
        } else {
            No<T> atual = head;
            while (atual.next != null) {
                atual = atual.next;
            }
            atual.next = novo;
        }
        tamanho++;
    }

    public void inserirOrdenado(T dado, Comparator<T> comparator) {
        No<T> novo = new No<>(dado);
        if (head == null || comparator.compare(dado, head.dado) <= 0) {
            novo.next = head;
            head = novo;
            tamanho++;
            return;
        }
        No<T> atual = head;
        while (atual.next != null && comparator.compare(dado, atual.next.dado) > 0) {
            atual = atual.next;
        }
        novo.next = atual.next;
        atual.next = novo;
        tamanho++;
    }

    public boolean remover(Predicate<T> predicado) {
        if (head == null) return false;
        if (predicado.test(head.dado)) {
            head = head.next;
            tamanho--;
            return true;
        }
        No<T> atual = head;
        while (atual.next != null) {
            if (predicado.test(atual.next.dado)) {
                atual.next = atual.next.next;
                tamanho--;
                return true;
            }
            atual = atual.next;
        }
        return false;
    }

    public T buscar(Predicate<T> predicado) {
        No<T> atual = head;
        while (atual != null) {
            if (predicado.test(atual.dado)) {
                return atual.dado;
            }
            atual = atual.next;
        }
        return null;
    }

    public int tamanho() {
        return tamanho;
    }

    public boolean vazia() {
        return head == null;
    }

    public List<T> paraLista() {
        List<T> lista = new ArrayList<>();
        No<T> atual = head;
        while (atual != null) {
            lista.add(atual.dado);
            atual = atual.next;
        }
        return lista;
    }
}
