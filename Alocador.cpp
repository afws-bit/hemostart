#ifndef ALOCADOR_CPP
#define ALOCADOR_CPP

#include <iostream>
#include <string>
#include "Compatibilidade.cpp"
#include "TabelaHash.cpp"
#include "Grafo.cpp"

// =====================================================================
// AED - Unidade 2: INTEGRACAO dos quatro algoritmos
// Dado um pedido (hospital, tipo do paciente, quantidade):
//   1. Compatibilidade: percorre os 8 tipos e usa MATRIZ[doador][receptor];
//   2. Hash: para cada tipo compativel, busca sua FilaFEFO em O(1) medio;
//   3. FEFO: primeiro esgota o MESMO tipo do paciente (poupa o O- universal),
//      depois escolhe, entre os outros tipos compativeis, sempre a bolsa
//      que vence primeiro (compara a frente de cada fila FEFO);
//   4. Rota: calcula hemocentro -> hospital pelo vizinho mais proximo.
// =====================================================================

const int MAX_ALOCADAS = 20;

struct Pedido {
    std::string hospital;
    std::string tipoPaciente;
    int quantidade;
};

struct ResultadoAlocacao {
    BolsaFEFO bolsas[MAX_ALOCADAS];
    int qtdBolsas;
    Rota rota;
    bool atendido;
};

class Alocador {
private:
    TabelaHashEstoque& estoque;
    GrafoHospitalar& mapa;
    std::string origem;

    // Retira do tipo quantas bolsas puder (da frente da FEFO) ate completar o pedido
    void retirarDoTipo(const std::string& tipo, ResultadoAlocacao& res, int quantidade) {
        FilaFEFO* fila = estoque.buscar(tipo);           // hash
        while (fila != nullptr && !fila->vazia() && res.qtdBolsas < quantidade) {
            fila->retirarProximaAVencer(res.bolsas[res.qtdBolsas]); // FEFO
            res.qtdBolsas++;
        }
    }

public:
    Alocador(TabelaHashEstoque& e, GrafoHospitalar& m, const std::string& hemocentro)
        : estoque(e), mapa(m), origem(hemocentro) {}

    // Antes de retirar, confere se ha bolsas suficientes (nao deixa pedido pela metade)
    ResultadoAlocacao alocar(const Pedido& p) {
        ResultadoAlocacao res;
        res.qtdBolsas = 0;
        res.atendido = false;
        res.rota.encontrada = false;
        res.rota.qtdParadas = 0;
        res.rota.distanciaTotal = 0;

        if (p.quantidade <= 0 || p.quantidade > MAX_ALOCADAS || indiceTipo(p.tipoPaciente) < 0) return res;

        int disponiveis = 0;
        for (int d = 0; d < QTD_TIPOS; d++) {
            if (compativel(TIPOS[d], p.tipoPaciente)) disponiveis += estoque.quantidade(TIPOS[d]);
        }
        if (disponiveis < p.quantidade) return res;      // estoque insuficiente

        retirarDoTipo(p.tipoPaciente, res, p.quantidade); // 1o: mesmo tipo (FEFO)

        // 2o: outros compativeis. A cada bolsa, olha a FRENTE da fila FEFO de cada
        // tipo compativel e retira a que vence primeiro (FEFO entre os tipos).
        while (res.qtdBolsas < p.quantidade) {
            FilaFEFO* melhor = nullptr;
            for (int d = 0; d < QTD_TIPOS; d++) {
                if (TIPOS[d] == p.tipoPaciente || !compativel(TIPOS[d], p.tipoPaciente)) continue;
                FilaFEFO* fila = estoque.buscar(TIPOS[d]);
                if (fila == nullptr || fila->vazia()) continue;
                if (melhor == nullptr
                        || fila->proximaAVencer()->dataValidade < melhor->proximaAVencer()->dataValidade) {
                    melhor = fila;
                }
            }
            melhor->retirarProximaAVencer(res.bolsas[res.qtdBolsas++]); // nunca nulo: total ja conferido
        }

        res.rota = mapa.rotaVizinhoMaisProximo(origem, p.hospital);
        res.atendido = true;
        return res;
    }
};

#endif
