

public class Historico {
    private static class Node {
        String acao;
        Node next;

        Node(String acao) {
            this.acao = acao;
            this.next = null;
        }
    }

    private Node head;
    private int tamanho;

    public Historico() {
        this.head = null;
        this.tamanho = 0;
    }

    public void registrar(String acao) {
        Node novoNode = new Node(acao);
        novoNode.next = head;
        head = novoNode;
        tamanho++;
    }

    public void imprimirHistorico() {
        Node atual = head;
        System.out.println("--- Histórico de Ações ---");
        while (atual != null) {
            System.out.println("- " + atual.acao);
            atual = atual.next;
        }
        System.out.println("--------------------------");
    }

    public int getTamanho() {
        return tamanho;
    }
}