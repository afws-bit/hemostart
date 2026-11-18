package br.com.rotavital.aed;

import br.com.rotavital.model.BolsaSangue;

import java.util.ArrayList;
import java.util.List;

/**
 * AED - Unidade 2: INTEGRAÇÃO dos quatro algoritmos (tradução de Alocador.cpp).
 * Dado um pedido (hospital, tipo do paciente, quantidade):
 *  1. Compatibilidade: MatrizCompatibilidade para cada um dos 8 tipos;
 *  2. Hash: TabelaHashEstoque.buscar(tipo) devolve a fila FEFO do tipo;
 *  3. FEFO: esgota primeiro o MESMO tipo do paciente; depois, entre os outros tipos
 *     compatíveis, retira sempre a bolsa que vence primeiro (compara a frente de cada fila);
 *  4. Rota: GrafoRotas.rotaVizinhoMaisProximo(hemocentro, hospital).
 * Se não há bolsas compatíveis suficientes, nada é retirado (pedido não atendido).
 *   Custo: O(8 + q·8) para escolher as q bolsas + O(V²) da rota.
 */
public class AlocadorBolsas {

    private final TabelaHashEstoque estoque;
    private final GrafoRotas mapa;
    private final String origem;

    public AlocadorBolsas(TabelaHashEstoque estoque, GrafoRotas mapa, String hemocentro) {
        this.estoque = estoque;
        this.mapa = mapa;
        this.origem = hemocentro;
    }

    public ResultadoAlocacao alocar(String hospital, String tipoPaciente, int quantidade) {
        if (quantidade <= 0 || MatrizCompatibilidade.indiceTipo(tipoPaciente) < 0) {
            return ResultadoAlocacao.naoAtendido();
        }

        int disponiveis = 0;
        for (String tipo : MatrizCompatibilidade.TIPOS) {
            if (MatrizCompatibilidade.compativel(tipo, tipoPaciente)) disponiveis += estoque.quantidade(tipo);
        }
        if (disponiveis < quantidade) return ResultadoAlocacao.naoAtendido();

        List<BolsaSangue> bolsas = new ArrayList<>();

        // 1º: mesmo tipo do paciente, em ordem FEFO
        FilaFEFO mesmoTipo = estoque.buscar(tipoPaciente);
        while (mesmoTipo != null && !mesmoTipo.vazia() && bolsas.size() < quantidade) {
            bolsas.add(mesmoTipo.retirarProximaAVencer());
        }

        // 2º: outros compatíveis — a cada bolsa, a frente de fila que vence primeiro
        while (bolsas.size() < quantidade) {
            FilaFEFO melhor = null;
            for (String tipo : MatrizCompatibilidade.TIPOS) {
                if (tipo.equals(tipoPaciente) || !MatrizCompatibilidade.compativel(tipo, tipoPaciente)) continue;
                FilaFEFO fila = estoque.buscar(tipo);
                if (fila == null || fila.vazia()) continue;
                if (melhor == null || fila.proximaAVencer().getDataValidade()
                        .isBefore(melhor.proximaAVencer().getDataValidade())) {
                    melhor = fila;
                }
            }
            bolsas.add(melhor.retirarProximaAVencer()); // nunca nulo: total já conferido
        }

        Rota rota = mapa.rotaVizinhoMaisProximo(origem, hospital);
        return new ResultadoAlocacao(List.copyOf(bolsas), rota, true);
    }
}
