package br.com.rotavital.observabilidade;

import br.com.rotavital.model.TipoSanguineo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Métricas de negócio do banco de sangue (ponto adicional do projeto de observabilidade).
 *
 * <table>
 *   <tr><th>Métrica (Prometheus)</th><th>Tipo</th><th>O que mede</th></tr>
 *   <tr><td>hemostart_bolsas_coletadas_total{tipo,componente}</td><td>counter</td><td>bolsas criadas na coleta</td></tr>
 *   <tr><td>hemostart_bolsas_distribuidas_total{tipo}</td><td>counter</td><td>bolsas entregues aos hospitais</td></tr>
 *   <tr><td>hemostart_bolsas_descartadas_total{tipo}</td><td>counter</td><td>bolsas descartadas (vencidas)</td></tr>
 *   <tr><td>hemostart_solicitacoes_criadas_total{tipo,urgencia}</td><td>counter</td><td>pedidos recebidos dos hospitais</td></tr>
 *   <tr><td>hemostart_solicitacoes_decididas_total{resultado}</td><td>counter</td><td>pedidos aprovados ou recusados</td></tr>
 *   <tr><td>hemostart_alocacao_duracao_seconds{resultado}</td><td>timer</td><td>tempo para aprovar um pedido e reservar as bolsas</td></tr>
 *   <tr><td>hemostart_estoque_disponivel{tipo}</td><td>gauge</td><td>bolsas disponíveis por tipo sanguíneo</td></tr>
 *   <tr><td>hemostart_solicitacoes_pendentes</td><td>gauge</td><td>pedidos aguardando decisão</td></tr>
 * </table>
 *
 * Os gauges leem o banco com SQL direto (no máximo uma consulta a cada 10 s, com cache), para que
 * cada coleta do Prometheus não gere spans de repositório no Tempo.
 */
@Component
public class MetricasNegocio {

    private static final Logger log = LoggerFactory.getLogger(MetricasNegocio.class);
    static final long CACHE_MS = 10_000;

    private final MeterRegistry registry;
    private final JdbcTemplate jdbc;
    private final Map<String, AtomicLong> estoquePorTipo = new ConcurrentHashMap<>();
    private final AtomicLong pendentes = new AtomicLong();
    private volatile long ultimaLeitura = 0;

    public MetricasNegocio(MeterRegistry registry, DataSource dataSource) {
        this.registry = registry;
        this.jdbc = new JdbcTemplate(dataSource);
        for (TipoSanguineo tipo : TipoSanguineo.values()) {
            String rotulo = tipo.getRotulo();
            AtomicLong valor = estoquePorTipo.computeIfAbsent(rotulo, r -> new AtomicLong());
            Gauge.builder("hemostart.estoque.disponivel", () -> {
                        atualizarGauges();
                        return valor.get();
                    })
                    .description("Bolsas de sangue disponíveis no estoque, por tipo sanguíneo")
                    .tag("tipo", rotulo)
                    .strongReference(true)
                    .register(registry);
        }
        Gauge.builder("hemostart.solicitacoes.pendentes", () -> {
                    atualizarGauges();
                    return pendentes.get();
                })
                .description("Solicitações de hospitais aguardando aprovação ou recusa")
                .strongReference(true)
                .register(registry);
    }

    public void bolsaColetada(String tipo, String componente) {
        Counter.builder("hemostart.bolsas.coletadas")
                .description("Bolsas de sangue registradas na coleta")
                .tag("tipo", valor(tipo))
                .tag("componente", valor(componente))
                .register(registry)
                .increment();
    }

    public void bolsasDistribuidas(String tipo, int quantidade) {
        Counter.builder("hemostart.bolsas.distribuidas")
                .description("Bolsas de sangue entregues aos hospitais")
                .tag("tipo", valor(tipo))
                .register(registry)
                .increment(quantidade);
    }

    public void bolsaDescartada(String tipo) {
        Counter.builder("hemostart.bolsas.descartadas")
                .description("Bolsas de sangue descartadas (vencidas ou impróprias)")
                .tag("tipo", valor(tipo))
                .register(registry)
                .increment();
    }

    public void solicitacaoCriada(String tipo, boolean urgente) {
        Counter.builder("hemostart.solicitacoes.criadas")
                .description("Solicitações de sangue recebidas dos hospitais")
                .tag("tipo", valor(tipo))
                .tag("urgencia", urgente ? "urgente" : "normal")
                .register(registry)
                .increment();
    }

    public void solicitacaoDecidida(String resultado) {
        Counter.builder("hemostart.solicitacoes.decididas")
                .description("Solicitações aprovadas ou recusadas pelo gestor")
                .tag("resultado", resultado)
                .register(registry)
                .increment();
    }

    /** Timer da operação crítica: aprovar um pedido e reservar bolsas compatíveis (FEFO). */
    public Timer timerAlocacao(String resultado) {
        return Timer.builder("hemostart.alocacao.duracao")
                .description("Tempo para aprovar uma solicitação e reservar as bolsas compatíveis")
                .tag("resultado", resultado)
                .register(registry);
    }

    /** Relê os gauges no banco, no máximo a cada {@link #CACHE_MS}. Falhas mantêm o último valor. */
    void atualizarGauges() {
        long agora = System.currentTimeMillis();
        if (agora - ultimaLeitura < CACHE_MS) {
            return;
        }
        synchronized (this) {
            if (agora - ultimaLeitura < CACHE_MS) {
                return;
            }
            ultimaLeitura = agora;
            try {
                estoquePorTipo.values().forEach(v -> v.set(0));
                jdbc.query("SELECT tipo_sanguineo, COUNT(*) FROM bolsa_sangue WHERE status = 'DISPONIVEL' "
                                + "GROUP BY tipo_sanguineo",
                        (RowCallbackHandler) rs -> {
                            AtomicLong v = estoquePorTipo.get(rs.getString(1));
                            if (v != null) {
                                v.set(rs.getLong(2));
                            }
                        });
                Long p = jdbc.queryForObject("SELECT COUNT(*) FROM requisicao WHERE status = 'PENDENTE'", Long.class);
                pendentes.set(p != null ? p : 0);
            } catch (RuntimeException e) {
                log.debug("Não foi possível atualizar as métricas de estoque: {}", e.getMessage());
            }
        }
    }

    private static String valor(String v) {
        return v == null || v.isBlank() ? "desconhecido" : v;
    }
}
