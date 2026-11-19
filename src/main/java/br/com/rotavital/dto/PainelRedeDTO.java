package br.com.rotavital.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dados do painel de monitoramento de rede (RSD, U2), servidos em GET /api/monitoramento/rede.
 *
 * @param geradoEm         momento do cálculo
 * @param janelaSegundos   janela de tempo considerada nas métricas
 * @param ligacoes         uma linha por ligação do diagrama de arquitetura que a aplicação consegue medir
 * @param serieHttp        últimas medições do tempo de resposta HTTP (para o gráfico)
 * @param serieBanco       últimas medições da sonda Aplicação → banco (para o gráfico)
 * @param rotas            tráfego HTTP agrupado por rota
 */
public record PainelRedeDTO(
        LocalDateTime geradoEm,
        long janelaSegundos,
        List<Ligacao> ligacoes,
        List<Ponto> serieHttp,
        List<Ponto> serieBanco,
        List<Rota> rotas) {

    /**
     * Métricas de uma ligação. Latências e jitter em ms; perda em %; vazão em medições por segundo.
     * {@code situacao} é OK, ATENCAO ou CRITICO conforme as metas do plano de implantação.
     */
    public record Ligacao(
            String nome,
            String origem,
            String destino,
            String protocolo,
            long amostras,
            double latenciaMedia,
            double latenciaP50,
            double latenciaP95,
            double latenciaP99,
            double jitter,
            double perdaPercentual,
            double vazaoPorSegundo,
            String situacao) {
    }

    public record Ponto(long instanteMs, double latenciaMs, boolean sucesso) {
    }

    public record Rota(String rota, long requisicoes, double latenciaMedia, double latenciaP95, long falhas) {
    }
}
