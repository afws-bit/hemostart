package br.com.rotavital.monitoramento;

import br.com.rotavital.dto.PainelRedeDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.com.rotavital.monitoramento.EstatisticasRede.arredondar;

/**
 * Guarda as medições das ligações de rede da aplicação e calcula as métricas do painel.
 *
 * <p>Ligações medidas (as setas do diagrama de arquitetura que passam pela aplicação):</p>
 * <ul>
 *   <li><b>Cliente → Aplicação (HTTP/8080)</b>: tempo de resposta de cada requisição,
 *       medido pelo {@link MetricasHttpFilter}. HTTP 5xx conta como perda.</li>
 *   <li><b>Aplicação → Banco (TCP/5432)</b>: ida e volta de um {@code SELECT 1},
 *       medida pela {@link SondaBanco}. Timeout ou erro de conexão conta como perda.</li>
 * </ul>
 * <p>A ligação Internet → borda HTTPS/443 fica fora do processo Java e é medida pelo
 * script de benchmarking (docs/rsd/benchmark).</p>
 */
@Component
public class MonitorRede {

    /** Metas do plano de implantação (docs/rsd/plano-implantacao-migracao.md, seção 5). */
    static final double META_HTTP_P95_MS = 300;
    static final double ALERTA_HTTP_P95_MS = 800;
    static final double META_BANCO_P95_MS = 20;
    static final double ALERTA_BANCO_P95_MS = 100;
    static final double META_PERDA_PCT = 1;
    static final double ALERTA_PERDA_PCT = 5;

    private static final int PONTOS_GRAFICO = 60;
    private static final Pattern URL_JDBC = Pattern.compile("^jdbc:(\\w+):(?://([^/:;?]+)(?::(\\d+))?)?.*");
    private static final Pattern SEGMENTO_ID = Pattern.compile(
            "^(\\d+|[0-9a-fA-F]{8}-[0-9a-fA-F-]{27,}|BS-\\d+|[0-9a-fA-F]{24,})$");

    private final SerieMedicoes http = new SerieMedicoes(5000);
    private final SerieMedicoes banco = new SerieMedicoes(720);
    private final String destinoBanco;
    private final String protocoloBanco;

    public MonitorRede(@Value("${spring.datasource.url:}") String urlBanco) {
        String[] destino = descreverBanco(urlBanco);
        this.destinoBanco = destino[0];
        this.protocoloBanco = destino[1];
    }

    public void registrarHttp(String metodo, String caminho, int status, double latenciaMs) {
        http.adicionar(new Amostra(System.currentTimeMillis(), latenciaMs, status < 500,
                metodo + " " + normalizarRota(caminho)));
    }

    public void registrarSondaBanco(double latenciaMs, boolean sucesso) {
        banco.adicionar(new Amostra(System.currentTimeMillis(), latenciaMs, sucesso, null));
    }

    public PainelRedeDTO painel(long janelaSegundos) {
        long agora = System.currentTimeMillis();
        long janelaMs = Math.max(10, janelaSegundos) * 1000L;
        List<Amostra> httpJanela = http.desde(agora - janelaMs);
        List<Amostra> bancoJanela = banco.desde(agora - janelaMs);

        List<PainelRedeDTO.Ligacao> ligacoes = List.of(
                ligacao("Cliente → Aplicação", "Navegador / Hospital (via borda)", "Aplicação HemoStart",
                        "HTTP/1.1 · 8080 (tempo de resposta no servidor)", httpJanela, agora, janelaMs,
                        META_HTTP_P95_MS, ALERTA_HTTP_P95_MS),
                ligacao("Aplicação → Banco", "Aplicação HemoStart", destinoBanco,
                        protocoloBanco + " (sonda SELECT 1)", bancoJanela, agora, janelaMs,
                        META_BANCO_P95_MS, ALERTA_BANCO_P95_MS));

        return new PainelRedeDTO(LocalDateTime.now(), janelaMs / 1000, ligacoes,
                pontos(httpJanela), pontos(bancoJanela), rotas(httpJanela));
    }

    private PainelRedeDTO.Ligacao ligacao(String nome, String origem, String destino, String protocolo,
                                          List<Amostra> amostras, long agora, long janelaMs,
                                          double metaP95, double alertaP95) {
        List<Double> lat = EstatisticasRede.latenciasComSucesso(amostras);
        double p95 = EstatisticasRede.percentil(lat, 95);
        double perda = EstatisticasRede.perdaPercentual(amostras);
        return new PainelRedeDTO.Ligacao(nome, origem, destino, protocolo, amostras.size(),
                arredondar(EstatisticasRede.media(lat), 2),
                arredondar(EstatisticasRede.percentil(lat, 50), 2),
                arredondar(p95, 2),
                arredondar(EstatisticasRede.percentil(lat, 99), 2),
                arredondar(EstatisticasRede.jitter(lat), 2),
                arredondar(perda, 2),
                arredondar(EstatisticasRede.vazaoPorSegundo(amostras, agora, janelaMs), 3),
                situacao(amostras.isEmpty(), p95, perda, metaP95, alertaP95));
    }

    static String situacao(boolean semDados, double p95, double perda, double metaP95, double alertaP95) {
        if (semDados) {
            return "SEM_DADOS";
        }
        if (p95 > alertaP95 || perda > ALERTA_PERDA_PCT) {
            return "CRITICO";
        }
        if (p95 > metaP95 || perda > META_PERDA_PCT) {
            return "ATENCAO";
        }
        return "OK";
    }

    private static List<PainelRedeDTO.Ponto> pontos(List<Amostra> amostras) {
        int inicio = Math.max(0, amostras.size() - PONTOS_GRAFICO);
        List<PainelRedeDTO.Ponto> pontos = new ArrayList<>();
        for (Amostra a : amostras.subList(inicio, amostras.size())) {
            pontos.add(new PainelRedeDTO.Ponto(a.instanteMs(), arredondar(a.latenciaMs(), 2), a.sucesso()));
        }
        return pontos;
    }

    private static List<PainelRedeDTO.Rota> rotas(List<Amostra> amostras) {
        Map<String, List<Amostra>> porRota = new LinkedHashMap<>();
        for (Amostra a : amostras) {
            porRota.computeIfAbsent(a.rota(), r -> new ArrayList<>()).add(a);
        }
        List<PainelRedeDTO.Rota> rotas = new ArrayList<>();
        porRota.forEach((rota, lista) -> {
            List<Double> lat = EstatisticasRede.latenciasComSucesso(lista);
            long falhas = lista.stream().filter(a -> !a.sucesso()).count();
            rotas.add(new PainelRedeDTO.Rota(rota, lista.size(),
                    arredondar(EstatisticasRede.media(lat), 2),
                    arredondar(EstatisticasRede.percentil(lat, 95), 2), falhas));
        });
        rotas.sort(Comparator.comparingLong(PainelRedeDTO.Rota::requisicoes).reversed());
        return rotas.size() > 15 ? rotas.subList(0, 15) : rotas;
    }

    /** Troca identificadores do caminho por {id} para agrupar, ex.: /api/v1/bolsas/BS-00001 → /api/v1/bolsas/{id}. */
    static String normalizarRota(String caminho) {
        if (caminho == null || caminho.isBlank()) {
            return "/";
        }
        String[] partes = caminho.split("/");
        StringBuilder sb = new StringBuilder();
        for (String parte : partes) {
            if (parte.isEmpty()) {
                continue;
            }
            sb.append('/').append(SEGMENTO_ID.matcher(parte).matches() ? "{id}" : parte);
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

    /** [destino, protocolo] a partir da URL JDBC. A senha nunca faz parte da URL usada aqui. */
    static String[] descreverBanco(String urlJdbc) {
        if (urlJdbc == null || urlJdbc.isBlank()) {
            return new String[]{"Banco de dados", "JDBC"};
        }
        // remove credenciais embutidas (jdbc:postgresql://usuario:senha@host/db) antes de exibir
        String semCredenciais = urlJdbc.replaceFirst("//[^/@]*@", "//");
        Matcher m = URL_JDBC.matcher(semCredenciais);
        if (!m.matches()) {
            return new String[]{"Banco de dados", "JDBC"};
        }
        String tipo = m.group(1).toLowerCase();
        if ("postgresql".equals(tipo)) {
            String porta = m.group(3) != null ? m.group(3) : "5432";
            return new String[]{"PostgreSQL (" + m.group(2) + ":" + porta + ")",
                    "protocolo PostgreSQL sobre TCP · " + porta};
        }
        if ("h2".equals(tipo)) {
            return new String[]{"H2 (local, mesmo processo)", "JDBC embarcado (sem rede)"};
        }
        return new String[]{tipo, "JDBC"};
    }

    SerieMedicoes serieHttp() {
        return http;
    }

    SerieMedicoes serieBanco() {
        return banco;
    }
}
