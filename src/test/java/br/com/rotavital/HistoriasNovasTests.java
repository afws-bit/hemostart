package br.com.rotavital;

import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.*;
import br.com.rotavital.repository.*;
import br.com.rotavital.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HU11 a HU14 (agendamento, triagem, aprovação/recusa e histórico do doador).
 * Todos os dados são lidos e gravados no banco (H2 em memória nos testes);
 * cada teste roda numa transação desfeita ao final.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HistoriasNovasTests {

    @Autowired private MockMvc mockMvc;

    @Autowired private DoadorRepository doadorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private BolsaSangueRepository bolsaRepository;
    @Autowired private HistoricoOperacaoRepository historicoRepository;

    @Autowired private DoadorService doadorService;
    @Autowired private AgendamentoService agendamentoService;
    @Autowired private TriagemService triagemService;
    @Autowired private EstoqueService estoqueService;
    @Autowired private RequisicaoService requisicaoService;
    @Autowired private ConfiguracaoService configuracaoService;

    private int sequencial = 0;

    @BeforeEach
    void setup() {
        configuracaoService.obter().setLimiteVagasPorHorario(2);
    }

    // ---------- helpers ----------

    private Doador criarDoador(String nome, String cpf, String sexo, String tipo, double peso, LocalDate ultimaDoacao) {
        Doador d = new Doador();
        d.setNome(nome);
        d.setCpf(cpf);
        d.setDataNascimento(LocalDate.now().minusYears(25));
        d.setSexo(sexo);
        d.setTipoSanguineo(tipo);
        d.setPeso(peso);
        d.setContato("(11) 99999-9999");
        d.setUltimaDoacao(ultimaDoacao);
        return doadorRepository.save(d);
    }

    private Hospital criarHospital(String cnpj, String nome) {
        Hospital h = new Hospital();
        h.setCnpj(cnpj);
        h.setRazaoSocial(nome);
        h.setEndereco("Recife/PE");
        h.setContato("(81) 3333-0000");
        h.setAtivo(true);
        return hospitalRepository.save(h);
    }

    private BolsaSangue criarBolsa(Doador doador, LocalDate coleta) {
        sequencial++;
        return bolsaRepository.save(BolsaSangue.coletar(String.format("BS-9%04d", sequencial), doador,
                Componente.CONCENTRADO_HEMACIAS, 450, coleta));
    }

    private void criarBolsasDisponiveis(String tipo, int quantidade) {
        Doador d = criarDoador("Doador " + tipo, String.format("9%010d", sequencial + 1000), "M", tipo, 80.0, null);
        for (int i = 0; i < quantidade; i++) criarBolsa(d, LocalDate.now());
    }

    // =========================================================================
    // HU11 — Agendamento de Doação
    // =========================================================================

    @Test
    void hu11_cenario1_agendamentoComDoadorAptoEHorarioDisponivel() {
        Doador maria = criarDoador("Maria Silva", "11122233344", "F", "O+", 60.0, null);
        LocalDate dataDesejada = LocalDate.now().plusDays(5);

        agendamentoService.agendar(maria.getId(), dataDesejada, "09:00");

        List<Agendamento> agendamentos = agendamentoService.listar();
        assertEquals(1, agendamentos.size());
        assertEquals("agendado", agendamentos.get(0).getStatus());
        assertEquals("09:00", agendamentos.get(0).getHorario());
        assertEquals(dataDesejada, agendamentos.get(0).getData());
    }

    @Test
    void hu11_cenario2_tentativaAgendamentoEmHorarioSemVagas() {
        LocalDate dia = LocalDate.now().plusDays(3);
        Doador d1 = criarDoador("Doador 1", "00100200301", "M", "A+", 70.0, null);
        Doador d2 = criarDoador("Doador 2", "00100200302", "F", "B+", 65.0, null);
        Doador d3 = criarDoador("Doador 3", "00100200303", "M", "O+", 80.0, null);

        agendamentoService.agendar(d1.getId(), dia, "09:00");
        agendamentoService.agendar(d2.getId(), dia, "09:00");

        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> agendamentoService.agendar(d3.getId(), dia, "09:00"));
        assertEquals("Horário indisponível", e.getMessage());

        long ativos = agendamentoService.listar().stream()
                .filter(a -> a.isAgendado() && a.getHorario().equals("09:00"))
                .count();
        assertEquals(2, ativos);
    }

    @Test
    void hu11_cenario3_tentativaAgendamentoAntesDoIntervaloMinimo() {
        Doador joao = criarDoador("João Souza", "12345678909", "M", "A+", 75.0, LocalDate.now().minusDays(30));

        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> agendamentoService.agendar(joao.getId(), LocalDate.now().plusDays(1), "10:00"));
        assertEquals("Doador ainda não apto para nova doação", e.getMessage());
        assertTrue(agendamentoService.listar().isEmpty());
    }

    @Test
    void hu11_cenario4_cancelamentoDeAgendamentoLiberaVaga() {
        configuracaoService.obter().setLimiteVagasPorHorario(1);
        LocalDate dia = LocalDate.now().plusDays(2);
        Doador d1 = criarDoador("Doador 1", "00100200301", "M", "A+", 70.0, null);
        Doador d2 = criarDoador("Doador 2", "00100200302", "F", "B+", 65.0, null);

        Agendamento ag = agendamentoService.agendar(d1.getId(), dia, "14:00");
        assertEquals("agendado", ag.getStatus());

        agendamentoService.cancelar(ag.getId());
        assertEquals("cancelado", agendamentoService.listar().get(0).getStatus());

        assertDoesNotThrow(() -> agendamentoService.agendar(d2.getId(), dia, "14:00"),
                "Vaga deveria estar disponível após cancelamento");
    }

    // =========================================================================
    // HU12 — Triagem Clínica Pré-Doação
    // =========================================================================

    @Test
    void hu12_cenario1_triagemComTodosParametrosDentroDoPadrao() {
        Doador maria = criarDoador("Maria Silva", "11122233344", "F", "O-", 62.0, null);

        TriagemClinica triagem = triagemService.registrarTriagem(maria.getId(), 62.0, "120/80", 36.5, 13.2);
        assertEquals("apto", triagem.getResultado());
        assertNull(triagem.getMotivoInaptidao());

        assertDoesNotThrow(() -> estoqueService.registrarColeta(maria.getId(), "Concentrado de Hemácias", 450),
                "Coleta deveria ter sido registrada com sucesso");
    }

    @Test
    void hu12_cenario2_triagemComHemoglobinaAbaixoDoMinimo() {
        Doador ana = criarDoador("Ana Lima", "99988877766", "F", "A+", 58.0, null);

        TriagemClinica triagem = triagemService.registrarTriagem(ana.getId(), 58.0, "110/70", 36.6, 11.8);
        assertEquals("inapto", triagem.getResultado());
        assertEquals("Hemoglobina abaixo do mínimo", triagem.getMotivoInaptidao());
    }

    @Test
    void hu12_cenario3_tentativaDeColetaComDoadorInapto() {
        Doador joao = criarDoador("João Souza", "12345678909", "M", "O+", 72.0, null);
        triagemService.registrarTriagem(joao.getId(), 72.0, "130/85", 36.8, 12.0);

        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> estoqueService.registrarColeta(joao.getId(), "Concentrado de Hemácias", 450));
        assertEquals("Doador inapto na triagem", e.getMessage());
    }

    // =========================================================================
    // HU13 — Aprovação e Recusa de Solicitações de Sangue
    // =========================================================================

    @Test
    void hu13_cenario1_aprovacaoDeSolicitacaoComEstoqueSuficiente() {
        criarHospital("12345678000199", "Hospital Teste");
        criarBolsasDisponiveis("O-", 5);

        Requisicao req = requisicaoService.criarRequisicao("12345678000199", "O-", 3, false);
        assertEquals(StatusRequisicao.PENDENTE, req.getStatus());

        requisicaoService.aprovarRequisicao(req.getId());
        assertEquals(StatusRequisicao.APROVADA, requisicaoService.buscarPorId(req.getId()).getStatus());

        long reservadas = bolsaRepository.findByRequisicao_IdAndStatus(req.getId(), StatusBolsa.RESERVADA).size();
        assertEquals(3, reservadas, "3 bolsas devem estar reservadas para a requisição");
    }

    @Test
    void hu13_cenario2_aprovacaoComEstoqueInsuficiente() {
        criarHospital("98765432000199", "Hospital Teste 2");
        criarBolsasDisponiveis("O-", 2);

        Requisicao req = requisicaoService.criarRequisicao("98765432000199", "O-", 3, false);

        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> requisicaoService.aprovarRequisicao(req.getId()));
        assertEquals("Estoque insuficiente para atender a solicitação", e.getMessage());
        assertEquals(StatusRequisicao.PENDENTE, requisicaoService.buscarPorId(req.getId()).getStatus());
    }

    @Test
    void hu13_cenario3_recusaDeSolicitacaoComMotivo() {
        criarHospital("55544433000199", "Hospital Teste 3");
        Requisicao req = requisicaoService.criarRequisicao("55544433000199", "A+", 2, false);

        String motivo = "Documentação incompleta do hospital solicitante";
        requisicaoService.recusarRequisicao(req.getId(), motivo);

        Requisicao lida = requisicaoService.buscarPorId(req.getId());
        assertEquals(StatusRequisicao.RECUSADA, lida.getStatus());
        assertEquals(motivo, lida.getMotivoRecusa());

        HistoricoOperacao hist = historicoRepository.findAll().stream()
                .filter(op -> op.getTipo() == TipoOperacao.RECUSA)
                .findFirst().orElse(null);
        assertNotNull(hist);
        assertTrue(hist.getDescricao().contains(motivo));
    }

    // =========================================================================
    // HU14 — Consulta de Histórico e Próxima Data Apta do Doador
    // =========================================================================

    @Test
    void hu14_cenario1_consultaDeDoadorComDoacoesAnteriores() {
        LocalDate trintaDiasAtras = LocalDate.now().minusDays(30);
        Doador joao = criarDoador("João Souza", "12345678909", "M", "O+", 78.0, trintaDiasAtras);
        criarBolsa(joao, trintaDiasAtras);

        List<BolsaSangue> doacoes = doadorService.listarDoacoesDoador(joao.getId());
        assertEquals(1, doacoes.size());
        assertEquals(trintaDiasAtras, doacoes.get(0).getDataColeta());
        assertEquals("O+", doacoes.get(0).getTipoSanguineo());
        assertEquals(450, doacoes.get(0).getVolume());

        assertEquals(trintaDiasAtras.plusDays(60), doadorService.calcularProximaDataApta(joao), "Homem deve aguardar 60 dias");
        assertFalse(doadorService.estaApto(joao), "Com 30 dias desde a última doação, ainda não está apto");
    }

    @Test
    void hu14_cenario2_consultaDeDoadorSemDoacoesAnteriores() {
        Doador carlos = criarDoador("Carlos Pereira", "99900011122", "M", "AB+", 82.0, null);

        assertTrue(doadorService.listarDoacoesDoador(carlos.getId()).isEmpty(), "Não deve haver doações registradas");
        assertTrue(doadorService.estaApto(carlos), "Doador sem doações anteriores deve aparecer como apto");
    }

    // =========================================================================
    // Integração Web (Controllers & Templates via MockMvc)
    // =========================================================================

    @Test
    void web_hu11_rotasDeAgendamento() throws Exception {
        Doador d = criarDoador("Lucas Mendes", "55566677788", "M", "O+", 75.0, null);

        mockMvc.perform(get("/agendamentos")).andExpect(status().isOk()).andExpect(view().name("agendamentos/lista"));
        mockMvc.perform(get("/agendamentos/novo")).andExpect(status().isOk()).andExpect(view().name("agendamentos/form"));
        mockMvc.perform(post("/agendamentos/salvar")
                        .param("doadorId", d.getId())
                        .param("data", LocalDate.now().plusDays(2).toString())
                        .param("horario", "09:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/agendamentos"))
                .andExpect(flash().attributeExists("sucesso"));
    }

    @Test
    void web_hu12_rotasDeTriagem() throws Exception {
        Doador d = criarDoador("Juliana Prado", "33344455566", "F", "A-", 63.0, null);

        mockMvc.perform(get("/triagem")).andExpect(status().isOk()).andExpect(view().name("triagem/lista"));
        mockMvc.perform(get("/triagem/nova")).andExpect(status().isOk()).andExpect(view().name("triagem/form"));
        mockMvc.perform(post("/triagem/salvar")
                        .param("doadorId", d.getId())
                        .param("peso", "63.0")
                        .param("pressaoArterial", "12/8")
                        .param("temperatura", "36.6")
                        .param("hemoglobina", "13.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/triagem"))
                .andExpect(flash().attributeExists("sucesso"));
    }

    @Test
    void web_hu13_rotasAprovarERecusar() throws Exception {
        criarHospital("77788899000100", "Hospital Web");
        criarBolsasDisponiveis("A+", 3);

        Requisicao req = requisicaoService.criarRequisicao("77788899000100", "A+", 2, false);
        mockMvc.perform(post("/requisicoes/aprovar/" + req.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requisicoes"))
                .andExpect(flash().attributeExists("sucesso"));

        Requisicao req2 = requisicaoService.criarRequisicao("77788899000100", "A+", 1, false);
        mockMvc.perform(post("/requisicoes/recusar/" + req2.getId()).param("motivo", "Falta de transporte no hospital"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requisicoes"))
                .andExpect(flash().attributeExists("sucesso"));

        mockMvc.perform(get("/requisicoes")).andExpect(status().isOk()).andExpect(view().name("requisicoes/fila"));
    }

    @Test
    void web_hu14_rotaHistoricoDoador() throws Exception {
        Doador d = criarDoador("Renata Borges", "44455566677", "F", "B+", 68.0, null);

        mockMvc.perform(get("/doadores/" + d.getId() + "/historico"))
                .andExpect(status().isOk())
                .andExpect(view().name("doadores/historico"))
                .andExpect(model().attributeExists("doador", "doacoes", "proximaData", "apto"));
    }
}
