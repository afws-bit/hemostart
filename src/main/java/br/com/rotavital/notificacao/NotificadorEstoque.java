package br.com.rotavital.notificacao;

import br.com.rotavital.model.Configuracao;
import br.com.rotavital.model.StatusBolsa;
import br.com.rotavital.model.TipoSanguineo;
import br.com.rotavital.service.CompatibilidadeService;
import br.com.rotavital.service.ConfiguracaoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RSD (U2) — notificações de estoque crítico em tempo real.
 *
 * <p>Guarda as conexões WebSocket abertas em {@code /ws/estoque} e, sempre que o estoque muda
 * (coleta, reserva, distribuição, descarte, vencimento ou mudança do mínimo configurado),
 * recalcula a situação de cada tipo sanguíneo e envia para todos os navegadores conectados.
 * A tela atualiza sem recarregar a página.</p>
 *
 * <p>Mensagem enviada (JSON):</p>
 * <pre>
 * {"evento":"estoque","momento":"2026-11-19T16:10:00",
 *  "situacao":[{"tipo":"O-","disponivel":1,"minimo":3,"critico":true}, ...],
 *  "mudancas":[{"tipo":"O-","evento":"ENTROU_EM_CRITICO","disponivel":1,"minimo":3}]}
 * </pre>
 * {@code mudancas} só lista os tipos que cruzaram o limite mínimo desde a última mensagem.
 */
@Service
public class NotificadorEstoque {

    private static final Logger log = LoggerFactory.getLogger(NotificadorEstoque.class);

    /** Tempo máximo para enviar uma mensagem a um cliente lento e tamanho máximo do buffer dele. */
    private static final int LIMITE_ENVIO_MS = 5_000;
    private static final int LIMITE_BUFFER_BYTES = 64 * 1024;

    public static final String ENTROU_EM_CRITICO = "ENTROU_EM_CRITICO";
    public static final String SAIU_DE_CRITICO = "SAIU_DE_CRITICO";

    public record SituacaoTipo(String tipo, long disponivel, int minimo, boolean critico) { }

    public record Mudanca(String tipo, String evento, long disponivel, int minimo) { }

    public record MensagemEstoque(String evento, String momento, List<SituacaoTipo> situacao, List<Mudanca> mudancas) { }

    private final CompatibilidadeService compatibilidadeService;
    private final ConfiguracaoService configuracaoService;
    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessoes = new ConcurrentHashMap<>();
    private Map<String, SituacaoTipo> ultimaSituacao = Map.of();

    public NotificadorEstoque(CompatibilidadeService compatibilidadeService,
                              ConfiguracaoService configuracaoService,
                              ObjectMapper objectMapper) {
        this.compatibilidadeService = compatibilidadeService;
        this.configuracaoService = configuracaoService;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------- conexões

    /** Novo navegador conectado: guarda a sessão e já envia a situação atual do estoque. */
    public void registrar(WebSocketSession sessao) {
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(sessao, LIMITE_ENVIO_MS, LIMITE_BUFFER_BYTES);
        sessoes.put(sessao.getId(), segura);
        log.info("[WS] Cliente conectado em /ws/estoque ({} conexão(ões) abertas)", sessoes.size());
        MensagemEstoque retrato;
        synchronized (this) {
            if (ultimaSituacao.isEmpty()) {
                ultimaSituacao = calcularSituacao();
            }
            retrato = mensagem(ultimaSituacao, List.of());
        }
        enviar(segura, paraJson(retrato));
    }

    public void remover(WebSocketSession sessao) {
        if (sessoes.remove(sessao.getId()) != null) {
            log.info("[WS] Cliente desconectado ({} conexão(ões) abertas)", sessoes.size());
        }
    }

    public int conexoesAbertas() {
        return sessoes.size();
    }

    // ---------------------------------------------------------------- verificação

    /**
     * Recalcula a situação do estoque e, se algo mudou desde a última mensagem, envia para todos
     * os clientes. Chamado pelo {@link NotificacaoEstoqueAspect} depois de cada operação que mexe
     * no estoque e, como garantia, pela verificação periódica.
     *
     * @return a mensagem enviada, ou vazio se nada mudou ou não há ninguém conectado
     */
    public synchronized Optional<MensagemEstoque> verificarEPublicar() {
        if (sessoes.isEmpty()) {
            ultimaSituacao = Map.of();   // recalcula do zero quando alguém conectar
            return Optional.empty();
        }
        Map<String, SituacaoTipo> nova = calcularSituacao();
        if (nova.equals(ultimaSituacao)) {
            return Optional.empty();
        }
        List<Mudanca> mudancas = compararMudancas(ultimaSituacao, nova);
        ultimaSituacao = nova;
        for (Mudanca m : mudancas) {
            if (ENTROU_EM_CRITICO.equals(m.evento())) {
                log.warn("[WS] Estoque crítico para {}: {} disponível(is), mínimo {}", m.tipo(), m.disponivel(), m.minimo());
            } else {
                log.info("[WS] Estoque de {} voltou ao normal: {} disponível(is), mínimo {}", m.tipo(), m.disponivel(), m.minimo());
            }
        }
        MensagemEstoque msg = mensagem(nova, mudancas);
        String json = paraJson(msg);
        sessoes.values().forEach(s -> enviar(s, json));
        return Optional.of(msg);
    }

    @Scheduled(fixedDelayString = "${notificacao.estoque.intervalo.ms:30000}",
               initialDelayString = "${notificacao.estoque.intervalo.ms:30000}")
    public void verificacaoPeriodica() {
        try {
            verificarEPublicar();
        } catch (RuntimeException e) {
            log.warn("[WS] Falha na verificação periódica do estoque: {}", e.getMessage());
        }
    }

    /** Situação atual de cada um dos 8 tipos, na ordem A+, A-, B+, ... */
    @SuppressWarnings("unchecked")
    Map<String, SituacaoTipo> calcularSituacao() {
        Map<String, Object> dados = compatibilidadeService.gerarPainelEstoque();
        Map<String, Map<String, Long>> painel = (Map<String, Map<String, Long>>) dados.get("painel");
        Configuracao config = configuracaoService.obter();
        Map<String, SituacaoTipo> situacao = new LinkedHashMap<>();
        for (String tipo : TipoSanguineo.rotulos()) {
            Long qtd = painel.get(tipo) == null ? null : painel.get(tipo).get(StatusBolsa.DISPONIVEL.getChave());
            long disponivel = qtd == null ? 0 : qtd;
            int minimo = config.minimoPara(tipo);
            situacao.put(tipo, new SituacaoTipo(tipo, disponivel, minimo, config.estoqueCritico(tipo, disponivel)));
        }
        return situacao;
    }

    /** Tipos que cruzaram o limite mínimo (entraram em crítico ou saíram dele). */
    static List<Mudanca> compararMudancas(Map<String, SituacaoTipo> anterior, Map<String, SituacaoTipo> atual) {
        List<Mudanca> mudancas = new ArrayList<>();
        if (anterior.isEmpty()) {
            return mudancas;  // primeiro retrato: não há "antes" para comparar
        }
        for (SituacaoTipo agora : atual.values()) {
            SituacaoTipo antes = anterior.get(agora.tipo());
            if (antes == null || antes.critico() == agora.critico()) {
                continue;
            }
            String evento = agora.critico() ? ENTROU_EM_CRITICO : SAIU_DE_CRITICO;
            mudancas.add(new Mudanca(agora.tipo(), evento, agora.disponivel(), agora.minimo()));
        }
        return mudancas;
    }

    // ---------------------------------------------------------------- envio

    private static MensagemEstoque mensagem(Map<String, SituacaoTipo> situacao, List<Mudanca> mudancas) {
        return new MensagemEstoque("estoque", LocalDateTime.now().withNano(0).toString(),
                List.copyOf(situacao.values()), mudancas);
    }

    private String paraJson(MensagemEstoque msg) {
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível gerar o JSON da notificação de estoque", e);
        }
    }

    private void enviar(WebSocketSession sessao, String json) {
        if (!sessao.isOpen()) {
            remover(sessao);
            return;
        }
        try {
            sessao.sendMessage(new TextMessage(json));
        } catch (IOException | RuntimeException e) {
            log.debug("[WS] Falha ao enviar para a sessão {}: {}", sessao.getId(), e.getMessage());
            remover(sessao);
        }
    }
}
