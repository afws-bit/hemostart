package br.com.rotavital.notificacao;

import br.com.rotavital.service.ConfiguracaoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RSD (U2) — WebSocket de estoque crítico ponta a ponta: sobe o servidor numa porta real,
 * conecta um cliente WebSocket em /ws/estoque e confere que (1) a situação chega logo ao conectar
 * e (2) o aviso chega sozinho, sem o cliente pedir, quando um tipo entra em estoque crítico.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificacaoEstoqueWebSocketTest {

    private static final String TIPO = "AB-";

    @LocalServerPort private int porta;
    @Autowired private ConfiguracaoService configuracaoService;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void clienteRecebeSituacaoAoConectarEAvisoQuandoTipoEntraEmCritico() throws Exception {
        BlockingQueue<String> recebidas = new LinkedBlockingQueue<>();
        TextWebSocketHandler cliente = new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                recebidas.add(message.getPayload());
            }
        };
        int minimoOriginal = configuracaoService.obter().minimoPara(TIPO);
        WebSocketSession sessao = new StandardWebSocketClient()
                .execute(cliente, "ws://localhost:" + porta + "/ws/estoque")
                .get(10, TimeUnit.SECONDS);
        try {
            String texto = recebidas.poll(10, TimeUnit.SECONDS);
            assertNotNull(texto, "o servidor deve enviar a situação do estoque logo após a conexão");
            JsonNode primeira = objectMapper.readTree(texto);
            assertEquals("estoque", primeira.get("evento").asText());
            assertEquals(8, primeira.get("situacao").size(), "um item por tipo sanguíneo");

            configuracaoService.definirMinimo(TIPO, 0);      // garante que o tipo não está crítico
            configuracaoService.definirMinimo(TIPO, 9_999);  // nenhum estoque atinge: vira crítico

            JsonNode aviso = esperarMudanca(recebidas, NotificadorEstoque.ENTROU_EM_CRITICO);
            assertNotNull(aviso, "o servidor deve avisar sozinho que " + TIPO + " entrou em estoque crítico");
            assertEquals(9_999, aviso.get("minimo").asInt());
            assertTrue(aviso.get("disponivel").asLong() < 9_999);
        } finally {
            configuracaoService.definirMinimo(TIPO, minimoOriginal);
            sessao.close();
        }
    }

    /** Lê as mensagens que chegarem em até 10 s e devolve a mudança do tipo testado, se houver. */
    private JsonNode esperarMudanca(BlockingQueue<String> recebidas, String evento) throws Exception {
        long limite = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < limite) {
            String texto = recebidas.poll(500, TimeUnit.MILLISECONDS);
            if (texto == null) {
                continue;
            }
            for (JsonNode mudanca : objectMapper.readTree(texto).get("mudancas")) {
                if (TIPO.equals(mudanca.get("tipo").asText()) && evento.equals(mudanca.get("evento").asText())) {
                    return mudanca;
                }
            }
        }
        return null;
    }
}
