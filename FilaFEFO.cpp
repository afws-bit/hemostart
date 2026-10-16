#include <iostream>
#include <string>

struct BolsaFEFO {
    int id;
    std::string tipoSanguineo;
    int quantidade;
    int dataValidade; 
    BolsaFEFO* proximo;
};

class FilaFEFO {
private:
    BolsaFEFO* inicio;

public:
    FilaFEFO() {
        inicio = nullptr;
    }

    
    void inserir(int id, std::string tipo, int qtd, int validade) {
        BolsaFEFO* nova = new BolsaFEFO();
        nova->id = id;
        nova->tipoSanguineo = tipo;
        nova->quantidade = qtd;
        nova->dataValidade = validade;
        nova->proximo = nullptr;

        
        if (inicio == nullptr || validade < inicio->dataValidade) {
            nova->proximo = inicio;
            inicio = nova;
            return;
        }

        
        BolsaFEFO* atual = inicio;
        while (atual->proximo != nullptr && atual->proximo->dataValidade <= validade) {
            atual = atual->proximo;
        }

        nova->proximo = atual->proximo;
        atual->proximo = nova;
    }

    
    void retirarProximaAVencer() {
        if (inicio == nullptr) {
            std::cout << "Estoque FEFO vazio.\n";
            return;
        }
        BolsaFEFO* temp = inicio;
        std::cout << "Bolsa retirada via FEFO -> ID: " << temp->id 
                  << " | Tipo: " << temp->tipoSanguineo 
                  << " | Validade: " << temp->dataValidade << "\n";
        inicio = inicio->proximo;
        delete temp;
    }

    void listar() {
        std::cout << "\n--- ESTOQUE FEFO (Ordenado por Validade) ---\n";
        BolsaFEFO* atual = inicio;
        while (atual != nullptr) {
            std::cout << "ID: " << atual->id 
                      << " | Tipo: " << atual->tipoSanguineo 
                      << " | Qtd: " << atual->quantidade 
                      << " | Validade: " << atual->dataValidade << "\n";
            atual = atual->proximo;
        }
    }
};