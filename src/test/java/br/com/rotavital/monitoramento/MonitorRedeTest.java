package br.com.rotavital.monitoramento;

import br.com.rotavital.dto.PainelRedeDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** RSD (U2) — regras do MonitorRede: agrupamento de rotas, leitura da URL do banco e situação OK/Atenção/Crítico. */
class MonitorRedeTest {

    @Test
    void identificadoresViramId() {
        assertEquals("/api/v1/bolsas/{id}/historico", MonitorRede.normalizarRota("/api/v1/bolsas/BS-00012/historico"));
        assertEquals("/api/v1/solicitacoes/{id}",
                MonitorRede.normalizarRota("/api/v1/solicitacoes/3f2b8c1a-9d4e-4b7a-8c11-2a6f0e9d1b33"));
        assertEquals("/hospitais/toggle/{id}", MonitorRede.normalizarRota("/hospitais/toggle/42"));
        assertEquals("/api/v1/estoque", MonitorRede.normalizarRota("/api/v1/estoque"));
        assertEquals("/", MonitorRede.normalizarRota("/"));
        assertEquals("/", MonitorRede.normalizarRota(""));
    }

    @Test
    void urlDoBancoViraDestinoSemCredenciais() {
        String[] pg = MonitorRede.descreverBanco("jdbc:postgresql://db:5432/rotavital");
        assertEquals("PostgreSQL (db:5432)", pg[0]);
        assertTrue(pg[1].contains("TCP"));

        String[] semPorta = MonitorRede.descreverBanco("jdbc:postgresql://dpg-abc123-a/hemo");
        assertEquals("PostgreSQL (dpg-abc123-a:5432)", semPorta[0]);

        String[] comSenha = MonitorRede.descreverBanco("jdbc:postgresql://usuario:segredo@dpg-x.render.com:5432/hemo");
        assertEquals("PostgreSQL (dpg-x.render.com:5432)", comSenha[0]);
        assertFalse(String.join(" ", comSenha).contains("segredo"));

        assertTrue(MonitorRede.descreverBanco("jdbc:h2:mem:teste")[0].startsWith("H2"));
        assertEquals("Banco de dados", MonitorRede.descreverBanco("")[0]);
    }

    @Test
    void situacaoSegueAsMetas() {
        assertEquals("SEM_DADOS", MonitorRede.situacao(true, 0, 0, 300, 800));
        assertEquals("OK", MonitorRede.situacao(false, 120, 0, 300, 800));
        assertEquals("ATENCAO", MonitorRede.situacao(false, 450, 0, 300, 800));
        assertEquals("ATENCAO", MonitorRede.situacao(false, 120, 2, 300, 800));
        assertEquals("CRITICO", MonitorRede.situacao(false, 900, 0, 300, 800));
        assertEquals("CRITICO", MonitorRede.situacao(false, 120, 6, 300, 800));
    }

    @Test
    void painelCalculaAsDuasLigacoes() {
        MonitorRede m = new MonitorRede("jdbc:postgresql://db:5432/rotavital");
        m.registrarHttp("GET", "/api/v1/bolsas/BS-00001", 200, 10);
        m.registrarHttp("GET", "/api/v1/bolsas/BS-00002", 200, 30);
        m.registrarHttp("GET", "/api/v1/bolsas/BS-00003", 503, 5);
        m.registrarHttp("GET", "/api/v1/bolsas/BS-00004", 404, 20);
        m.registrarSondaBanco(2.0, true);
        m.registrarSondaBanco(4.0, true);

        PainelRedeDTO p = m.painel(300);
        PainelRedeDTO.Ligacao http = p.ligacoes().get(0);
        PainelRedeDTO.Ligacao banco = p.ligacoes().get(1);

        assertEquals(4, http.amostras());
        assertEquals(25.0, http.perdaPercentual(), 1e-9);   // só o 503 é perda; 404 é resposta válida
        assertEquals(20.0, http.latenciaMedia(), 1e-9);     // (10 + 30 + 20) / 3
        assertEquals(15.0, http.jitter(), 1e-9);            // (|30-10| + |20-30|) / 2
        assertEquals("CRITICO", http.situacao());           // perda 25% > 5%
        assertEquals(1, p.rotas().size());
        assertEquals("GET /api/v1/bolsas/{id}", p.rotas().get(0).rota());
        assertEquals(1, p.rotas().get(0).falhas());

        assertEquals("PostgreSQL (db:5432)", banco.destino());
        assertEquals(3.0, banco.latenciaMedia(), 1e-9);
        assertEquals(2.0, banco.jitter(), 1e-9);
        assertEquals("OK", banco.situacao());
        assertEquals(2, p.serieBanco().size());
    }
}
