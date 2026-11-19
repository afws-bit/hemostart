package br.com.rotavital.notificacao;

import br.com.rotavital.notificacao.NotificadorEstoque.Mudanca;
import br.com.rotavital.notificacao.NotificadorEstoque.SituacaoTipo;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regra de quando avisar: só quando um tipo cruza o mínimo, nos dois sentidos. */
class NotificadorEstoqueTest {

    private static Map<String, SituacaoTipo> situacao(SituacaoTipo... itens) {
        Map<String, SituacaoTipo> mapa = new LinkedHashMap<>();
        for (SituacaoTipo s : itens) {
            mapa.put(s.tipo(), s);
        }
        return mapa;
    }

    @Test
    void avisaQuandoTipoEntraEmCriticoEQuandoVoltaAoNormal() {
        Map<String, SituacaoTipo> antes = situacao(
                new SituacaoTipo("O-", 3, 3, false),
                new SituacaoTipo("A+", 1, 2, true));
        Map<String, SituacaoTipo> depois = situacao(
                new SituacaoTipo("O-", 2, 3, true),
                new SituacaoTipo("A+", 2, 2, false));

        List<Mudanca> mudancas = NotificadorEstoque.compararMudancas(antes, depois);

        assertEquals(List.of(
                new Mudanca("O-", NotificadorEstoque.ENTROU_EM_CRITICO, 2, 3),
                new Mudanca("A+", NotificadorEstoque.SAIU_DE_CRITICO, 2, 2)), mudancas);
    }

    @Test
    void naoAvisaQuandoSoAQuantidadeMudaSemCruzarOMinimo() {
        Map<String, SituacaoTipo> antes = situacao(new SituacaoTipo("B+", 10, 3, false));
        Map<String, SituacaoTipo> depois = situacao(new SituacaoTipo("B+", 7, 3, false));

        assertTrue(NotificadorEstoque.compararMudancas(antes, depois).isEmpty());
    }

    @Test
    void primeiroRetratoNaoGeraAvisos() {
        Map<String, SituacaoTipo> atual = situacao(new SituacaoTipo("AB-", 0, 2, true));

        assertTrue(NotificadorEstoque.compararMudancas(Map.of(), atual).isEmpty());
    }
}
