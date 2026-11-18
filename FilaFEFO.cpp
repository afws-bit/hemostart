#ifndef FILA_FEFO_CPP
#define FILA_FEFO_CPP

#include <iostream>
#include <string>

// =====================================================================
// AED - Unidade 2: Priorizacao FEFO (First Expire, First Out)
// Lista encadeada mantida ORDENADA por data de validade (insercao ordenada).
//   inserir:               O(n)  - percorre ate achar a posicao
//   retirarProximaAVencer: O(1)  - a mais urgente esta sempre no inicio
// Datas no formato inteiro AAAAMMDD (ex.: 20261115), que preserva a ordem cronologica.
// =====================================================================

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
    int tamanho;

public:
    FilaFEFO() {
        inicio = nullptr;
        tamanho = 0;
    }

    void inserir(int id, std::string tipo, int qtd, int validade) {
        BolsaFEFO* nova = new BolsaFEFO();
        nova->id = id;
        nova->tipoSanguineo = tipo;
        nova->quantidade = qtd;
        nova->dataValidade = validade;
        nova->proximo = nullptr;
        tamanho++;

        // Caso 1: lista vazia ou a nova vence antes da primeira -> vira o inicio
        if (inicio == nullptr || validade < inicio->dataValidade) {
            nova->proximo = inicio;
            inicio = nova;
            return;
        }

        // Caso 2: anda ate o ultimo no com validade <= nova (empates mantem ordem de chegada)
        BolsaFEFO* atual = inicio;
        while (atual->proximo != nullptr && atual->proximo->dataValidade <= validade) {
            atual = atual->proximo;
        }

        nova->proximo = atual->proximo;
        atual->proximo = nova;
    }

    bool vazia() const { return inicio == nullptr; }
    int getTamanho() const { return tamanho; }

    // Consulta a bolsa mais urgente sem remover (nullptr se vazia). O(1)
    const BolsaFEFO* proximaAVencer() const { return inicio; }

    // Remove a bolsa mais urgente e copia seus dados em 'saida'. O(1)
    bool retirarProximaAVencer(BolsaFEFO& saida) {
        if (inicio == nullptr) return false;
        BolsaFEFO* temp = inicio;
        saida = *temp;
        saida.proximo = nullptr;
        inicio = inicio->proximo;
        delete temp; // liberacao manual
        tamanho--;
        return true;
    }

    // Versao da U1 (mantida): retira e imprime
    void retirarProximaAVencer() {
        BolsaFEFO b;
        if (!retirarProximaAVencer(b)) {
            std::cout << "Estoque FEFO vazio.\n";
            return;
        }
        std::cout << "Bolsa retirada via FEFO -> ID: " << b.id
                  << " | Tipo: " << b.tipoSanguineo
                  << " | Validade: " << b.dataValidade << "\n";
    }

    void listar() const {
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

    ~FilaFEFO() {
        while (inicio != nullptr) {
            BolsaFEFO* temp = inicio;
            inicio = inicio->proximo;
            delete temp;
        }
    }
};

#endif
