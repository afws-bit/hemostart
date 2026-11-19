package br.com.rotavital.monitoramento;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Mede o tempo de resposta de cada requisição HTTP que chega à aplicação (ligação Cliente → Aplicação,
 * porta 8080) e entrega ao {@link MonitorRede}.
 *
 * <p>Ficam de fora os arquivos estáticos, o console do H2 e o próprio endpoint do painel,
 * que é consultado a cada poucos segundos e distorceria as métricas.</p>
 */
@Component
public class MetricasHttpFilter extends OncePerRequestFilter {

    private final MonitorRede monitor;

    public MetricasHttpFilter(MonitorRede monitor) {
        this.monitor = monitor;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        return caminho.startsWith("/css/")
                || caminho.startsWith("/js/")
                || caminho.startsWith("/rede/")
                || caminho.startsWith("/h2-console")
                || caminho.startsWith("/api/monitoramento")
                || caminho.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.nanoTime();
        int status = 500;
        try {
            chain.doFilter(request, response);
            status = response.getStatus();
        } finally {
            double ms = (System.nanoTime() - inicio) / 1_000_000.0;
            String caminho = request.getRequestURI().substring(request.getContextPath().length());
            monitor.registrarHttp(request.getMethod(), caminho, status, ms);
        }
    }
}
