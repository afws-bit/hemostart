import java.time.LocalDate;

public class SistemaBancoDeSangue {
    private Estoque estoque;
    private GrafoCompatibilidade compatibilidade;
    private FilaPrioridade emergencias;
    private BSTValidade validades;
    private Historico historico;

    public SistemaBancoDeSangue() {
        this.estoque = new Estoque();
        this.compatibilidade = new GrafoCompatibilidade();
        this.emergencias = new FilaPrioridade();
        this.validades = new BSTValidade();
        this.historico = new Historico();
        
        inicializarRegrasCompatibilidade();
    }

    private void inicializarRegrasCompatibilidade() {
        
        compatibilidade.adicionarAresta("O-", "O+");
        compatibilidade.adicionarAresta("O-", "A+");
        compatibilidade.adicionarAresta("O-", "B+");
        compatibilidade.adicionarAresta("O-", "AB+");
        compatibilidade.adicionarAresta("A-", "A+");
        compatibilidade.adicionarAresta("A-", "AB+");
        compatibilidade.adicionarAresta("B-", "B+");
        compatibilidade.adicionarAresta("B-", "AB+");
        
    }

    public void registrarDoacao(String tipoSanguineo, int quantidade, LocalDate validade) {
        estoque.adicionar(tipoSanguineo, quantidade);
        validades.inserir(validade, tipoSanguineo);
        historico.registrar("Doação registrada: " + quantidade + " bolsas de " + tipoSanguineo + " (Vence em: " + validade + ")");
        System.out.println("✅ Doação registrada com sucesso!");
    }

    public void atenderEmergencia(String paciente, int prioridade) {
        emergencias.inserir(paciente, prioridade);
        historico.registrar("Emergência registrada: Paciente " + paciente + " (Prioridade: " + prioridade + ")");
    }

    public void chamarProximoEmergencia() {
        String paciente = emergencias.remover();
        System.out.println("🚨 Chamando paciente da fila de emergência: " + paciente);
        historico.registrar("Paciente atendido na emergência: " + paciente);
    }

    public void verificarCompatibilidade(String doador, String receptor) {
        boolean compativel = compatibilidade.validarDoacao(doador, receptor);
        if (compativel) {
            System.out.println("✅ O tipo " + doador + " é COMPATÍVEL com " + receptor);
        } else {
            System.out.println("❌ O tipo " + doador + " NÃO É COMPATÍVEL com " + receptor);
        }
    }

    public void mostrarEstoque(String tipo) {
        int qtd = estoque.buscar(tipo);
        System.out.println("📦 Estoque de " + tipo + ": " + qtd + " bolsas.");
    }

    public void mostrarVencimentoMaisProximo() {
        System.out.println("⏳ Bolsa que vence primeiro: " + validades.pegarVencimentoMaisProximo());
    }

    public void mostrarHistorico() {
        historico.imprimirHistorico();
    }
}