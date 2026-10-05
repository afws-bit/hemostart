package br.com.rotavital;

import br.com.rotavital.estruturas.ListaEncadeada;
import br.com.rotavital.estruturas.FilaRequisicoes;
import br.com.rotavital.estruturas.PilhaHistorico;
import br.com.rotavital.model.Requisicao;
import br.com.rotavital.service.CompatibilidadeService;
import br.com.rotavital.service.DoadorService;
import br.com.rotavital.model.Doador;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RotaVitalTests {

    // --- ListaEncadeada ---

    @Test
    void listaEncadeada_inserirFim() {
        ListaEncadeada<Integer> lista = new ListaEncadeada<>();
        lista.inserirFim(1);
        lista.inserirFim(2);
        lista.inserirFim(3);
        assertEquals(3, lista.tamanho());
        assertEquals(List.of(1, 2, 3), lista.paraLista());
    }

    @Test
    void listaEncadeada_inserirInicio() {
        ListaEncadeada<Integer> lista = new ListaEncadeada<>();
        lista.inserirInicio(3);
        lista.inserirInicio(2);
        lista.inserirInicio(1);
        assertEquals(List.of(1, 2, 3), lista.paraLista());
    }

    @Test
    void listaEncadeada_inserirOrdenado() {
        ListaEncadeada<Integer> lista = new ListaEncadeada<>();
        Comparator<Integer> cmp = Comparator.naturalOrder();
        lista.inserirOrdenado(3, cmp);
        lista.inserirOrdenado(1, cmp);
        lista.inserirOrdenado(2, cmp);
        assertEquals(List.of(1, 2, 3), lista.paraLista());
    }

    @Test
    void listaEncadeada_remover() {
        ListaEncadeada<Integer> lista = new ListaEncadeada<>();
        lista.inserirFim(1);
        lista.inserirFim(2);
        lista.inserirFim(3);
        assertTrue(lista.remover(x -> x == 2));
        assertEquals(List.of(1, 3), lista.paraLista());
        assertEquals(2, lista.tamanho());
    }

    @Test
    void listaEncadeada_buscar() {
        ListaEncadeada<String> lista = new ListaEncadeada<>();
        lista.inserirFim("ola");
        lista.inserirFim("mundo");
        assertEquals("mundo", lista.buscar(s -> s.equals("mundo")));
        assertNull(lista.buscar(s -> s.equals("nada")));
    }

    // --- FilaRequisicoes ---

    @Test
    void filaRequisicoes_ordemNormal() {
        FilaRequisicoes fila = new FilaRequisicoes();
        fila.enfileirar(makeReq("R1", false));
        fila.enfileirar(makeReq("R2", false));
        fila.enfileirar(makeReq("R3", false));
        assertEquals("R1", fila.desenfileirar().getId());
        assertEquals("R2", fila.desenfileirar().getId());
        assertEquals("R3", fila.desenfileirar().getId());
        assertTrue(fila.vazia());
    }

    @Test
    void filaRequisicoes_urgenteFrenteDosNormais() {
        FilaRequisicoes fila = new FilaRequisicoes();
        fila.enfileirar(makeReq("N1", false));
        fila.enfileirar(makeReq("N2", false));
        fila.enfileirar(makeReq("U1", true));
        // U1 urgente deve ir à frente de N1 e N2
        assertEquals("U1", fila.desenfileirar().getId());
        assertEquals("N1", fila.desenfileirar().getId());
        assertEquals("N2", fila.desenfileirar().getId());
    }

    @Test
    void filaRequisicoes_multiplosUrgentesMantemOrdemDeChegada() {
        FilaRequisicoes fila = new FilaRequisicoes();
        fila.enfileirar(makeReq("N1", false));
        fila.enfileirar(makeReq("U1", true));
        fila.enfileirar(makeReq("U2", true));
        fila.enfileirar(makeReq("N2", false));
        // U1 chegou antes de U2; N1 e N2 ficam por último
        assertEquals("U1", fila.desenfileirar().getId());
        assertEquals("U2", fila.desenfileirar().getId());
        assertEquals("N1", fila.desenfileirar().getId());
        assertEquals("N2", fila.desenfileirar().getId());
    }

    // --- PilhaHistorico ---

    @Test
    void pilhaHistorico_lifoOrdem() {
        PilhaHistorico<String> pilha = new PilhaHistorico<>();
        pilha.empilhar("A");
        pilha.empilhar("B");
        pilha.empilhar("C");
        assertEquals("C", pilha.topo());
        assertEquals(List.of("C", "B", "A"), pilha.paraLista());
        assertEquals("C", pilha.desempilhar());
        assertEquals(2, pilha.tamanho());
    }

    @Test
    void pilhaHistorico_vazia() {
        PilhaHistorico<Integer> pilha = new PilhaHistorico<>();
        assertTrue(pilha.vazia());
        assertNull(pilha.desempilhar());
    }

    // --- Compatibilidade Sanguínea ---

    @Test
    void compatibilidade_OMenosParaTodos() {
        CompatibilidadeService svc = new CompatibilidadeService(null);
        for (String receptor : new String[]{"O-","O+","A-","A+","B-","B+","AB-","AB+"}) {
            assertTrue(svc.compativel("O-", receptor),
                    "O- deve ser compatível com " + receptor);
        }
    }

    @Test
    void compatibilidade_ABMaisApenasABMais() {
        CompatibilidadeService svc = new CompatibilidadeService(null);
        assertTrue(svc.compativel("AB+", "AB+"));
        assertFalse(svc.compativel("AB+", "A+"));
        assertFalse(svc.compativel("AB+", "O+"));
        assertFalse(svc.compativel("AB+", "AB-"));
    }

    @Test
    void compatibilidade_AMenosParaAeAB() {
        CompatibilidadeService svc = new CompatibilidadeService(null);
        assertTrue(svc.compativel("A-", "A-"));
        assertTrue(svc.compativel("A-", "A+"));
        assertTrue(svc.compativel("A-", "AB-"));
        assertTrue(svc.compativel("A-", "AB+"));
        assertFalse(svc.compativel("A-", "O+"));
        assertFalse(svc.compativel("A-", "B+"));
    }

    // --- Intervalo entre doações ---

    @Test
    void intervaloDoacao_homem60Dias() {
        Doador d = new Doador();
        d.setSexo("M");
        d.setUltimaDoacao(LocalDate.now().minusDays(59));
        DoadorService svc = new DoadorService(null);
        assertFalse(svc.aptoPorIntervalo(d), "59 dias < 60 — não apto");

        d.setUltimaDoacao(LocalDate.now().minusDays(60));
        assertTrue(svc.aptoPorIntervalo(d), "60 dias — apto");
    }

    @Test
    void intervaloDoacao_mulher90Dias() {
        Doador d = new Doador();
        d.setSexo("F");
        d.setUltimaDoacao(LocalDate.now().minusDays(89));
        DoadorService svc = new DoadorService(null);
        assertFalse(svc.aptoPorIntervalo(d), "89 dias < 90 — não apta");

        d.setUltimaDoacao(LocalDate.now().minusDays(90));
        assertTrue(svc.aptoPorIntervalo(d), "90 dias — apta");
    }

    @Test
    void intervaloDoacao_semUltimaDoacao() {
        Doador d = new Doador();
        d.setSexo("M");
        DoadorService svc = new DoadorService(null);
        assertTrue(svc.aptoPorIntervalo(d), "Nunca doou — apto");
    }

    // helpers
    private Requisicao makeReq(String id, boolean urgente) {
        Requisicao r = new Requisicao();
        r.setId(id);
        r.setUrgente(urgente);
        r.setStatus("pendente");
        r.setDataSolicitacao(java.time.LocalDateTime.now());
        return r;
    }
}
