#include <iostream>
#include "ListaEstoque.cpp"
#include "FilaRequisicoes.cpp"
#include "PilhaHistorico.cpp"

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

    return 0;
}