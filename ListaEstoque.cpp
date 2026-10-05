#include <iostream>
#include <string>

struct Bolsa {
    int id;
    std::string tipoSanguineo;
    int quantidade;
    Bolsa* proximo; // Ponteiro explícito
};

class ListaEstoque {
private:
    Bolsa* inicio;

public:
    ListaEstoque() {
        inicio = nullptr;
    }

    void inserir(int id, std::string tipo, int qtd) {
        // Alocação manual de memória com 'new'
        Bolsa* nova = new Bolsa(); 
        nova->id = id;
        nova->tipoSanguineo = tipo;
        nova->quantidade = qtd;
        nova->proximo = inicio;
        inicio = nova;
    }

    void listar() {
        Bolsa* atual = inicio;
        std::cout << "--- ESTOQUE (Lista) ---\n";
        while (atual != nullptr) {
            std::cout << "ID: " << atual->id 
                      << " | Tipo: " << atual->tipoSanguineo 
                      << " | Qtd: " << atual->quantidade << "\n";
            atual = atual->proximo; // Navegação com ponteiro
        }
    }

    // Destrutor para liberar a memória manualmente
    ~ListaEstoque() {
        Bolsa* atual = inicio;
        while (atual != nullptr) {
            Bolsa* temp = atual;
            atual = atual->proximo;
            delete temp; // Liberação manual de memória
        }
    }
};