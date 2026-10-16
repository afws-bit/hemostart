#ifndef GRAFO_HPP
#define GRAFO_HPP

#include <iostream>
#include <vector>
#include <unordered_map>
#include <string>

struct Aresta {
    std::string destino;
    int pesoDistancia; 
};

class GrafoHospitalar {
private:
    std::unordered_map<std::string, std::vector<Aresta>> listaAdjacencia;

public:
    void adicionarVertice(const std::string& nomeLocal) {
        if (listaAdjacencia.find(nomeLocal) == listaAdjacencia.end()) {
            listaAdjacencia[nomeLocal] = std::vector<Aresta>();
        }
    }

    void adicionarRota(const std::string& origem, const std::string& destino, int distancia) {
        adicionarVertice(origem);
        adicionarVertice(destino);
        listaAdjacencia[origem].push_back({destino, distancia});
        listaAdjacencia[destino].push_back({origem, distancia}); 
    }

    void exibirRotas() const {
        for (const auto& [local, rotas] : listaAdjacencia) {
            std::cout << "Local: " << local << "\n";
            for (const auto& rota : rotas) {
                std::cout << "  -> Conecta a " << rota.destino << " (" << rota.pesoDistancia << " km)\n";
            }
        }
    }
};

#endif