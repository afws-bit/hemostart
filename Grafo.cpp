#ifndef GRAFO_HPP
#define GRAFO_HPP

#include <iostream>
#include <string>

// =====================================================================
// AED - Unidade 2: Roteirizacao por VIZINHO MAIS PROXIMO
// Grafo nao direcionado e ponderado (km) em MATRIZ DE ADJACENCIA.
// distancia[i][j] = 0 significa "sem estrada direta".
// rotaVizinhoMaisProximo: a partir da origem, vai sempre para o vizinho
// NAO visitado mais proximo; se o destino e vizinho direto, vai direto a ele.
// E uma heuristica gulosa: rapida, mas nao garante o menor caminho.
//   Complexidade: O(V^2) - ate V passos, cada um varre uma linha de V colunas.
// =====================================================================

const int MAX_LOCAIS = 20;
const int MAX_ROTA = MAX_LOCAIS;

struct Rota {
    std::string paradas[MAX_ROTA];
    int qtdParadas;
    int distanciaTotal;
    bool encontrada;
};

class GrafoHospitalar {
private:
    std::string nomes[MAX_LOCAIS];
    int distancia[MAX_LOCAIS][MAX_LOCAIS];
    int qtdLocais;

    // Busca sequencial do nome do local. O(V)
    int indice(const std::string& nome) const {
        for (int i = 0; i < qtdLocais; i++) {
            if (nomes[i] == nome) return i;
        }
        return -1;
    }

public:
    GrafoHospitalar() {
        qtdLocais = 0;
        for (int i = 0; i < MAX_LOCAIS; i++)
            for (int j = 0; j < MAX_LOCAIS; j++)
                distancia[i][j] = 0;
    }

    int adicionarVertice(const std::string& nomeLocal) {
        int i = indice(nomeLocal);
        if (i >= 0) return i;
        if (qtdLocais == MAX_LOCAIS) return -1;
        nomes[qtdLocais] = nomeLocal;
        return qtdLocais++;
    }

    void adicionarRota(const std::string& origem, const std::string& destino, int km) {
        int a = adicionarVertice(origem);
        int b = adicionarVertice(destino);
        if (a < 0 || b < 0) return;
        distancia[a][b] = km;
        distancia[b][a] = km; // nao direcionado
    }

    Rota rotaVizinhoMaisProximo(const std::string& origem, const std::string& destino) const {
        Rota rota;
        rota.qtdParadas = 0;
        rota.distanciaTotal = 0;
        rota.encontrada = false;

        int atual = indice(origem);
        int alvo = indice(destino);
        if (atual < 0 || alvo < 0) return rota;

        bool visitado[MAX_LOCAIS] = {false};
        visitado[atual] = true;
        rota.paradas[rota.qtdParadas++] = nomes[atual];

        while (atual != alvo) {
            int proximo = -1;
            if (distancia[atual][alvo] > 0) {
                proximo = alvo;                       // destino e vizinho direto
            } else {
                int menor = 0;
                for (int j = 0; j < qtdLocais; j++) { // vizinho nao visitado mais proximo
                    if (distancia[atual][j] > 0 && !visitado[j]
                            && (proximo == -1 || distancia[atual][j] < menor)) {
                        proximo = j;
                        menor = distancia[atual][j];
                    }
                }
            }
            if (proximo == -1) return rota;           // beco sem saida: rota nao encontrada
            rota.distanciaTotal += distancia[atual][proximo];
            visitado[proximo] = true;
            atual = proximo;
            rota.paradas[rota.qtdParadas++] = nomes[atual];
        }
        rota.encontrada = true;
        return rota;
    }

    void exibirRotas() const {
        for (int i = 0; i < qtdLocais; i++) {
            std::cout << "Local: " << nomes[i] << "\n";
            for (int j = 0; j < qtdLocais; j++) {
                if (distancia[i][j] > 0)
                    std::cout << "  -> Conecta a " << nomes[j] << " (" << distancia[i][j] << " km)\n";
            }
        }
    }
};

void imprimirRota(const Rota& r) {
    if (!r.encontrada) {
        std::cout << "Rota nao encontrada.\n";
        return;
    }
    for (int i = 0; i < r.qtdParadas; i++) {
        std::cout << r.paradas[i] << (i + 1 < r.qtdParadas ? " -> " : "");
    }
    std::cout << " (" << r.distanciaTotal << " km)\n";
}

#endif
