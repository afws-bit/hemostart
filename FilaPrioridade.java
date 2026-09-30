import java.util.ArrayList;
import java.util.List;

public class FilaPrioridade {
    private static class Paciente {
        String nome;
        int prioridade;

        Paciente(String nome, int prioridade) {
            this.nome = nome;
            this.prioridade = prioridade;
        }
    }

    private List<Paciente> heap;

    public FilaPrioridade() {
        this.heap = new ArrayList<>();
    }

    public void inserir(String nome, int prioridade) {
        Paciente novoPaciente = new Paciente(nome, prioridade);
        heap.add(novoPaciente);
        heapifyUp(heap.size() - 1);
    }

    public String remover() {
        if (heap.isEmpty()) return "Fila vazia";
        
        Paciente removido = heap.get(0);
        Paciente ultimo = heap.remove(heap.size() - 1);

        if (!heap.isEmpty()) {
            heap.set(0, ultimo);
            heapifyDown(0);
        }
        return removido.nome;
    }

    private void heapifyUp(int index) {
        while (index > 0) {
            int parentIndex = (index - 1) / 2;
            if (heap.get(index).prioridade >= heap.get(parentIndex).prioridade) break;
            
            Paciente temp = heap.get(index);
            heap.set(index, heap.get(parentIndex));
            heap.set(parentIndex, temp);
            index = parentIndex;
        }
    }

    private void heapifyDown(int index) {
        int tamanho = heap.size();
        while (true) {
            int menor = index;
            int esquerda = 2 * index + 1;
            int direita = 2 * index + 2;

            if (esquerda < tamanho && heap.get(esquerda).prioridade < heap.get(menor).prioridade) {
                menor = esquerda;
            }
            if (direita < tamanho && heap.get(direita).prioridade < heap.get(menor).prioridade) {
                menor = direita;
            }

            if (menor == index) break;

            Paciente temp = heap.get(index);
            heap.set(index, heap.get(menor));
            heap.set(menor, temp);
            index = menor;
        }
    }
}