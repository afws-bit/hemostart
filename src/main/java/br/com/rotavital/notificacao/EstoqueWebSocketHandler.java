package br.com.rotavital.notificacao;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Ponto de entrada do WebSocket {@code /ws/estoque}. O navegador abre a conexão (handshake HTTP
 * com {@code Upgrade: websocket}, resposta 101) e a partir daí o servidor empurra as mensagens.
 * O canal é só do servidor para o cliente: o que o cliente enviar é ignorado, exceto "ping".
 */
@Component
public class EstoqueWebSocketHandler extends TextWebSocketHandler {

    private final NotificadorEstoque notificador;

    public EstoqueWebSocketHandler(NotificadorEstoque notificador) {
        this.notificador = notificador;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        notificador.registrar(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equals(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        notificador.remover(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        notificador.remover(session);
    }
}
