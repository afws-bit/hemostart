package br.com.rotavital.notificacao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;

import java.util.Map;

/**
 * Registra o endpoint WebSocket {@code /ws/estoque}. Fica na mesma porta e no mesmo domínio da
 * aplicação (no Render: {@code wss://hemostart-rotavital-api.onrender.com/ws/estoque}, com TLS
 * terminado no proxy do Render), então não precisa liberar outra porta nem CORS.
 *
 * <p>O registro é feito à mão, sem {@code @EnableWebSocket}, porque essa anotação cria um
 * {@code TaskScheduler} próprio que desliga o agendador padrão do Spring Boot usado pelos
 * {@code @Scheduled} do projeto (rotinas de SO, validade e sonda de rede).</p>
 */
@Configuration
public class WebSocketConfig {

    public static final String CAMINHO = "/ws/estoque";

    @Bean
    public WebSocketHttpRequestHandler estoqueWebSocketEndpoint(EstoqueWebSocketHandler handler) {
        return new WebSocketHttpRequestHandler(handler, new DefaultHandshakeHandler());
    }

    @Bean
    public WebSocketHandlerMapping estoqueWebSocketMapping(WebSocketHttpRequestHandler estoqueWebSocketEndpoint) {
        WebSocketHandlerMapping mapping = new WebSocketHandlerMapping();  // também inicia o handshake handler (SmartLifecycle)
        mapping.setUrlMap(Map.of(CAMINHO, estoqueWebSocketEndpoint));
        mapping.setOrder(1);   // antes do mapeamento de recursos estáticos
        return mapping;
    }
}
