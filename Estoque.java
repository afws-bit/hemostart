public class Estoque {
    private static class Node {
        String chave;
        Integer valor;
        Node next;

        Node(String chave, Integer valor) {
            this.chave = chave;
            this.valor = valor;
            this.next = null;
        }
    }

    private Node[] tabela;
    private int capacidade = 16;

    public Estoque() {
        this.tabela = new Node[capacidade];
    }

    private int hash(String chave) {
        return Math.abs(chave.hashCode()) % capacidade;
    }

    public void adicionar(String tipoSanguineo, int quantidade) {
        int index = hash(tipoSanguineo);
        Node atual = tabela[index];

        while (atual != null) {
            if (atual.chave.equals(tipoSanguineo)) {
                atual.valor += quantidade;
                return;
            }
            atual = atual.next;
        }

        Node novoNode = new Node(tipoSanguineo, quantidade);
        novoNode.next = tabela[index];
        tabela[index] = novoNode;
    }

    public int buscar(String tipoSanguineo) {
        int index = hash(tipoSanguineo);
        Node atual = tabela[index];

        while (atual != null) {
            if (atual.chave.equals(tipoSanguineo)) {
                return atual.valor;
            }
            atual = atual.next;
        }
        return 0;
    }
}