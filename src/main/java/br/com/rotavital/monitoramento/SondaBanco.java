package br.com.rotavital.monitoramento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Sonda ativa da ligação Aplicação → Banco (TCP/5432 no PostgreSQL).
 *
 * <p>A cada {@code monitoramento.sonda.intervalo.ms} executa {@code SELECT 1} e mede a ida e volta.
 * Uma consulta tão simples quase não usa CPU do banco, então o tempo medido é dominado pela rede
 * e pelo pool de conexões. Timeout (2 s) ou erro de conexão é registrado como perda.</p>
 */
@Component
public class SondaBanco {

    private static final Logger log = LoggerFactory.getLogger(SondaBanco.class);
    static final int TIMEOUT_SEGUNDOS = 2;

    private final JdbcTemplate jdbc;
    private final MonitorRede monitor;

    public SondaBanco(DataSource dataSource, MonitorRede monitor) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(TIMEOUT_SEGUNDOS);
        this.monitor = monitor;
    }

    @Scheduled(fixedDelayString = "${monitoramento.sonda.intervalo.ms:5000}")
    public void medir() {
        long inicio = System.nanoTime();
        boolean sucesso;
        try {
            Integer um = jdbc.queryForObject("SELECT 1", Integer.class);
            sucesso = um != null && um == 1;
        } catch (RuntimeException e) {
            sucesso = false;
            log.warn("Sonda do banco falhou: {}", e.getMessage());
        }
        double ms = (System.nanoTime() - inicio) / 1_000_000.0;
        monitor.registrarSondaBanco(ms, sucesso);
    }
}
