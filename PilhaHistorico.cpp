#include <iostream>
#include <string>

struct AcaoHistorico {
    std::string acao;
    AcaoHistorico* proximo; // Ponteiro explícito
};

class PilhaHistorico {
private:
    AcaoHistorico* topo;

public:
    PilhaHistorico() {
        topo = nullptr;
    }

    void push(std::string acao) {
        // Alocação manual
        AcaoHistorico* nova = new AcaoHistorico();
        nova->acao = acao;
        nova->proximo = topo;
        topo = nova;
    }

    void pop() {
        if (topo == nullptr) return;
        
        AcaoHistorico* temp = topo;
        topo = topo->proximo;
        delete temp; // Liberação manual
    }

    void imprimir() {
        AcaoHistorico* atual = topo;
        std::cout << "--- HISTÓRICO (Pilha) ---\n";
        while (atual != nullptr) {
            std::cout << "- " << atual->acao << "\n";
            atual = atual->proximo;
        }
    }

    ~PilhaHistorico() {
        while (topo != nullptr) {
            pop();
        }
    }
};