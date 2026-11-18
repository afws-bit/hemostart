#ifndef TABELA_HASH_HPP
#define TABELA_HASH_HPP

#include <iostream>
#include <string>
#include "FilaFEFO.cpp"

// =====================================================================
// AED - Unidade 2: Indice de estoque por HASH
// Chave: tipo sanguineo ("O-", "A+"...). Valor: FilaFEFO com as bolsas daquele tipo.
// Tratamento de colisao: encadeamento separado (cada balde e uma lista ligada de nos).
// Funcao hash: djb2 (hash * 33 + c) modulo TAMANHO_TABELA (primo).
//   buscar/inserir: O(1) em media; O(k) no pior caso, k = nos no mesmo balde.
// Toda a memoria (nos e filas) e alocada com new e liberada com delete.
// =====================================================================

struct EntradaHash {
    std::string chave;      // tipo sanguineo
    FilaFEFO* bolsas;       // bolsas desse tipo, ordenadas por validade
    EntradaHash* proximo;   // encadeamento no mesmo balde
};

class TabelaHashEstoque {
private:
    static const int TAMANHO_TABELA = 101;
    EntradaHash* baldes[TAMANHO_TABELA];

public:
    TabelaHashEstoque() {
        for (int i = 0; i < TAMANHO_TABELA; i++) baldes[i] = nullptr;
    }

    // djb2: identico ao metodo funcaoHash() da versao Java
    int funcaoHash(const std::string& chave) const {
        unsigned long hash = 5381;
        for (char c : chave) {
            hash = ((hash << 5) + hash) + (unsigned char) c; // hash * 33 + c
        }
        return (int) (hash % TAMANHO_TABELA);
    }

    // Busca a fila FEFO do tipo. nullptr se o tipo nao tem bolsas. O(1) medio
    FilaFEFO* buscar(const std::string& tipo) const {
        EntradaHash* atual = baldes[funcaoHash(tipo)];
        while (atual != nullptr) {
            if (atual->chave == tipo) return atual->bolsas;
            atual = atual->proximo;
        }
        return nullptr;
    }

    // Insere a bolsa na fila FEFO do seu tipo (cria a entrada se for o primeiro). O(1) + O(n) da FEFO
    void inserir(int id, const std::string& tipo, int qtd, int validade) {
        FilaFEFO* fila = buscar(tipo);
        if (fila == nullptr) {
            int indice = funcaoHash(tipo);
            EntradaHash* nova = new EntradaHash();
            nova->chave = tipo;
            nova->bolsas = new FilaFEFO();
            nova->proximo = baldes[indice]; // insere no inicio do balde
            baldes[indice] = nova;
            fila = nova->bolsas;
        }
        fila->inserir(id, tipo, qtd, validade);
    }

    int quantidade(const std::string& tipo) const {
        FilaFEFO* fila = buscar(tipo);
        return fila == nullptr ? 0 : fila->getTamanho();
    }

    ~TabelaHashEstoque() {
        for (int i = 0; i < TAMANHO_TABELA; i++) {
            EntradaHash* atual = baldes[i];
            while (atual != nullptr) {
                EntradaHash* temp = atual;
                atual = atual->proximo;
                delete temp->bolsas;
                delete temp;
            }
        }
    }
};

#endif
