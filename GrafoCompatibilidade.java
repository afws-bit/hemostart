import java.util.*;

public class GrafoCompatibilidade {
    private Map<String, List<String>> adjacencias;

    public GrafoCompatibilidade() {
        this.adjacencias = new HashMap<>();
    }

    public void adicionarAresta(String doador, String receptor) {
        this.adjacencias.computeIfAbsent(doador, k -> new ArrayList<>()).add(receptor);
    }

    
    public boolean validarDoacao(String doador, String receptor) {
        if (doador.equals(receptor)) return true;
        if (!adjacencias.containsKey(doador)) return false;

        Queue<String> fila = new LinkedList<>();
        Set<String> visitados = new HashSet<>();

        fila.add(doador);
        visitados.add(doador);

        while (!fila.isEmpty()) {
            String atual = fila.poll();
            List<String> vizinhos = adjacencias.getOrDefault(atual, new ArrayList<>());

            for (String vizinho : vizinhos) {
                if (vizinho.equals(receptor)) {
                    return true;
                }
                if (!visitados.contains(vizinho)) {
                    visitados.add(vizinho);
                    fila.add(vizinho);
                }
            }
        }
        return false;
    }
}