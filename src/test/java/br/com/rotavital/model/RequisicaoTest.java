package br.com.rotavital.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Testes de unidade das transições de estado da Requisicao (sem Spring e sem banco). */
class RequisicaoTest {

    private Requisicao pendente() {
        return Requisicao.nova(null, "O-", 2, true);
    }

    @Test
    void novaRequisicaoComecaPendente() {
        Requisicao r = pendente();
        assertTrue(r.isPendente());
        assertNotNull(r.getDataSolicitacao());
    }

    @Test
    void aprovarRequisicaoPendente() {
        Requisicao r = pendente();
        r.aprovar();
        assertTrue(r.isAprovada());
    }

    @Test
    void naoAprovaRequisicaoJaAprovada() {
        Requisicao r = pendente();
        r.aprovar();
        assertThrows(IllegalStateException.class, r::aprovar);
    }

    @Test
    void recusarGuardaMotivoSemEspacos() {
        Requisicao r = pendente();
        r.recusar("  Estoque insuficiente  ");
        assertTrue(r.isRecusada());
        assertEquals("Estoque insuficiente", r.getMotivoRecusa());
    }

    @Test
    void recusarExigeMotivo() {
        Requisicao r = pendente();
        assertThrows(IllegalArgumentException.class, () -> r.recusar(" "));
        assertTrue(r.isPendente());
    }

    @Test
    void atenderRequisicaoAprovada() {
        Requisicao r = pendente();
        r.aprovar();
        r.atender("Maria");
        assertTrue(r.isAtendida());
        assertEquals("Maria", r.getResponsavelDistribuicao());
        assertNotNull(r.getDataAtendimento());
    }

    @Test
    void naoAtendeRequisicaoPendente() {
        Requisicao r = pendente();
        assertThrows(IllegalStateException.class, () -> r.atender("Maria"));
        assertTrue(r.isPendente());
    }
}
