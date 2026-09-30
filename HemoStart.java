import java.time.LocalDate;

public class HemoStart {
    public static void main(String[] args) {
        System.out.println("🩸 Bem-vindo ao HemoStart - Sistema de Banco de Sangue 🩸\n");

    
        SistemaBancoDeSangue sistema = new SistemaBancoDeSangue();

        
        System.out.println("--- Teste de Estoque (HashMap) ---");
       
        sistema.registrarDoacao("O-", 5, LocalDate.now().plusDays(30));
        sistema.registrarDoacao("A+", 10, LocalDate.now().plusDays(15));
        sistema.mostrarEstoque("O-");
        sistema.mostrarEstoque("A+");

        
        System.out.println("\n--- Teste de Compatibilidade (Grafo/BFS) ---");
        
        sistema.verificarCompatibilidade("O-", "A+");
        sistema.verificarCompatibilidade("A+", "O-");

        
        System.out.println("\n--- Teste de Validade (BST) ---");
        sistema.mostrarVencimentoMaisProximo();

        
        System.out.println("\n--- Teste de Emergências (MinHeap) ---");
        sistema.atenderEmergencia("João", 3);
        sistema.atenderEmergencia("Maria", 1); // 
        sistema.atenderEmergencia("Pedro", 2);
        sistema.chamarProximoEmergencia(); // 

       
        System.out.println("\n--- Histórico do Sistema (LinkedList) ---");
        sistema.mostrarHistorico();
    }
}