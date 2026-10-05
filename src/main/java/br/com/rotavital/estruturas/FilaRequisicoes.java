package br.com.rotavital.estruturas;

import br.com.rotavital.model.Requisicao;

import java.util.ArrayList;
import java.util.List;

public class FilaRequisicoes {

    private static class No {
        Requisicao dado;
        No next;

        No(Requisicao dado) {
            this.dado = dado;
            this.next = null;
        }
    }

    private No head;
    private No tail;
    private int tamanho;

    public FilaRequisicoes() {
        this.head = null;
        this.tail = null;
        this.tamanho = 0;
    }

    public void enfileirar(Requisicao requisicao) {
        No novo = new No(requisicao);
        if (requisicao.isUrgente()) {
            // Inserir após todos os urgentes existentes no inicio
            if (head == null) {
                head = novo;
                tail = novo;
                tamanho++;
                return;
            }
            if (!head.dado.isUrgente()) {
                // todos são normais, urgente vai na frente
                novo.next = head;
                head = novo;
                tamanho++;
                return;
            }
            // percorrer urgentes existentes
            No atual = head;
            while (atual.next != null && atual.next.dado.isUrgente()) {
                atual = atual.next;
            }
            novo.next = atual.next;
            atual.next = novo;
            if (novo.next == null) {
                tail = novo;
            }
            tamanho++;
        } else {
            // Normal vai no fim
            if (tail == null) {
                head = novo;
                tail = novo;
            } else {
                tail.next = novo;
                tail = novo;
            }
            tamanho++;
        }
    }

    public Requisicao desenfileirar() {
        if (head == null) return null;
        Requisicao dado = head.dado;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        tamanho--;
        return dado;
    }

    public Requisicao primeiro() {
        return head != null ? head.dado : null;
    }

    public boolean vazia() {
        return head == null;
    }

    public int tamanho() {
        return tamanho;
    }

    public List<Requisicao> paraLista() {
        List<Requisicao> lista = new ArrayList<>();
        No atual = head;
        while (atual != null) {
            lista.add(atual.dado);
            atual = atual.next;
        }
        return lista;
    }

    public void remover(String id) {
        if (head == null) return;
        if (head.dado.getId().equals(id)) {
            head = head.next;
            if (head == null) tail = null;
            tamanho--;
            return;
        }
        No atual = head;
        while (atual.next != null) {
            if (atual.next.dado.getId().equals(id)) {
                if (atual.next == tail) {
                    tail = atual;
                }
                atual.next = atual.next.next;
                tamanho--;
                return;
            }
            atual = atual.next;
        }
    }
}
