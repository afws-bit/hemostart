#include <iostream>
#include "ListaEstoque.cpp"
#include "FilaRequisicoes.cpp"
#include "PilhaHistorico.cpp"
#include "TabelaHash.cpp"
#include "Grafo.cpp"
#include "FilaFEFO.cpp"

int main() {
    std::cout << "🩸 Sistema HemoStart - Unidade 1 (C++) 🩸\n\n";

    // Testando Lista (Estoque)
    ListaEstoque estoque;
    estoque.inserir(1, "O-", 10);
    estoque.inserir(2, "A+", 5);
    estoque.listar();

    // Testando Fila (Requisições)
    FilaRequisicoes fila;
    fila.enfileirar(101, "Hospital Central");
    fila.enfileirar(102, "Hospital Sul");
    std::cout << "\nRequisições enfileiradas com sucesso.\n";

    // Testando Pilha (Histórico)
    PilhaHistorico historico;
    historico.push("Doacao registrada: O-");
    historico.push("Requisicao criada: Hospital Central");
    historico.imprimir();

    // Testando Tabela Hash (Estoque por Hash)
    std::cout << "\n--- Testando Tabela Hash (AED) ---\n";
    TabelaHash tabelaHash;
    tabelaHash.inserir("O-", 10);
    tabelaHash.inserir("A+", 5);
    tabelaHash.buscar("O-");

    // Testando Grafo (Rotas entre Hemocentro e Hospitais)
    std::cout << "\n--- Testando Grafo (AED) ---\n";
    Grafo grafo;
    grafo.adicionarAresta("Hemocentro", "Hospital Central", 12);
    grafo.adicionarAresta("Hemocentro", "Hospital Sul", 25);
    grafo.imprimir();

    // Testando FEFO (First Expire, First Out)
    std::cout << "\n--- Testando Gestao FEFO (AED) ---\n";
    FilaFEFO fefo;
    // Inserindo bolsas com validades diferentes fora de ordem
    fefo.inserir(201, "O+", 4, 20261115); // Vence em 15/11/2026
    fefo.inserir(202, "O+", 2, 20261020); // Vence em 20/10/2026 (vence primeiro!)
    fefo.inserir(203, "A-", 6, 20261030); // Vence em 30/10/2026

    fefo.listar();
    fefo.retirarProximaAVencer(); // Irá retirar a bolsa ID 202
    fefo.listar();

    return 0;
}