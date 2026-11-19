package br.com.rotavital.monitoramento;

/**
 * Uma medição de uma ligação de rede (RSD — painel de monitoramento).
 *
 * @param instanteMs  quando a medição terminou (epoch em ms)
 * @param latenciaMs  tempo medido, em milissegundos
 * @param sucesso     false quando a medição conta como perda (timeout, erro de conexão ou HTTP 5xx)
 * @param rota        agrupamento da medição (ex.: "GET /api/v1/bolsas/{id}"); null na sonda do banco
 */
public record Amostra(long instanteMs, double latenciaMs, boolean sucesso, String rota) {
}
