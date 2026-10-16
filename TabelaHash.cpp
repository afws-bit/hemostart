#ifndef TABELA_HASH_HPP
#define TABELA_HASH_HPP

#include <iostream>
#include <vector>
#include <list>
#include <string>

struct ItemEstoque {
    std::string id;
    std::string tipoSanguineo; 
    int quantidade;
    std::string dataValidade;  
};

class TabelaHashEstoque {
private:
    static const int TAMANHO_TABELA = 101; 
    std::list<ItemEstoque> tabela[TAMANHO_TABELA];

    int funcaoHash(const std::string& chave) const {
        unsigned long hash = 5381;
        for (char c : chave) {
            hash = ((hash << 5) + hash) + c; 
        }
        return hash % TAMANHO_TABELA;
    }

public:
    void inserir(const ItemEstoque& item) {
        int indice = funcaoHash(item.id);
        for (auto& elem : tabela[indice]) {
            if (elem.id == item.id) {
                elem.quantidade += item.quantidade; 
                return;
            }
        }
        tabela[indice].push_back(item);
    }

    ItemEstoque* buscar(const std::string& id) {
        int indice = funcaoHash(id);
        for (auto& item : tabela[indice]) {
            if (item.id == id) {
                return &item;
            }
        }
        return nullptr;
    }

    bool remover(const std::string& id) {
        int indice = funcaoHash(id);
        auto& lista = tabela[indice];
        for (auto it = lista.begin(); it != lista.end(); ++it) {
            if (it->id == id) {
                lista.erase(it);
                return true;
            }
        }
        return false;
    }
};

#endif