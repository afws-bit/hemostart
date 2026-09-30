import java.time.LocalDate;

public class BSTValidade {
    private static class Node {
        LocalDate validade;
        String tipoSanguineo;
        Node left, right;

        Node(LocalDate validade, String tipoSanguineo) {
            this.validade = validade;
            this.tipoSanguineo = tipoSanguineo;
            this.left = this.right = null;
        }
    }

    private Node root;

    public BSTValidade() {
        this.root = null;
    }

    public void inserir(LocalDate validade, String tipoSanguineo) {
        root = inserirRec(root, validade, tipoSanguineo);
    }

    private Node inserirRec(Node node, LocalDate validade, String tipoSanguineo) {
        if (node == null) {
            return new Node(validade, tipoSanguineo);
        }

        if (validade.isBefore(node.validade)) {
            node.left = inserirRec(node.left, validade, tipoSanguineo);
        } else if (validade.isAfter(node.validade)) {
            node.right = inserirRec(node.right, validade, tipoSanguineo);
        }
        return node;
    }

    public String pegarVencimentoMaisProximo() {
        if (root == null) return "Nenhuma bolsa cadastrada";
        
        Node atual = root;
        while (atual.left != null) {
            atual = atual.left;
        }
        return "Tipo: " + atual.tipoSanguineo + " | Vence em: " + atual.validade;
    }
}