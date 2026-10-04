#include <iostream>
#include <string>

struct Requisicao {
    int id;
    std::string hospital;
    Requisicao* proximo; // Ponteiro explícito
};

class FilaRequisicoes {
private:
    Requisicao* frente;
    Requisicao* tras;

public:
    FilaRequisicoes() {
        frente = nullptr;
        tras = nullptr;
    }

    void enfileirar(int id, std::string hospital) {
        // Alocação manual
        Requisicao* nova = new Requisicao();
        nova->id = id;
        nova->hospital = hospital;
        nova->proximo = nullptr;

        if (tras == nullptr) {
            frente = tras = nova;
        } else {
            tras->proximo = nova;
            tras = nova;
        }
    }

    void desenfileirar() {
        if (frente == nullptr) return;
        
        Requisicao* temp = frente;
        frente = frente->proximo;
        
        if (frente == nullptr) {
            tras = nullptr;
        }
        
        delete temp; // Liberação manual
    }

    ~FilaRequisicoes() {
        while (frente != nullptr) {
            desenfileirar();
        }
    }
};