package br.com.rotavital;

import br.com.rotavital.model.*;
import br.com.rotavital.repository.JsonDataRepository;
import br.com.rotavital.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HistoriasNovasTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonDataRepository repo;

    @Autowired
    private DoadorService doadorService;

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private TriagemService triagemService;

    @Autowired
    private EstoqueService estoqueService;

    @Autowired
    private RequisicaoService requisicaoService;

    @Autowired
    private CompatibilidadeService compatibilidadeService;

    @BeforeEach
    void setup() {
        repo.getLock().lock();
        try {
            repo.getAgendamentos().clear();
            repo.getTriagens().clear();
            repo.getDoadores().clear();
            repo.getHospitais().clear();
            repo.getTodasRequisicoes().clear();
            repo.getFilaRequisicoes().limpar();
            repo.getEstoque().limpar();
            repo.getHistoricoStack().limpar();
            repo.getConfiguracao().setLimiteVagasPorHorario(2);
        } finally {
            repo.getLock().unlock();
        }
    }

    private Doador criarDoador(String nome, String cpf, String sexo, String tipo, double peso, LocalDate ultimaDoacao) {
        Doador d = new Doador();
        d.setId(UUID.randomUUID().toString());
        d.setNome(nome);
        d.setCpf(cpf);
        d.setDataNascimento(LocalDate.now().minusYears(25));
        d.setSexo(sexo);
        d.setTipoSanguineo(tipo);
        d.setPeso(peso);
        d.setContato("(11) 99999-9999");
        d.setUltimaDoacao(ultimaDoacao);
        repo.getDoadores().add(d);
        return d;
    }

    private BolsaSangue criarBolsa(String tipo, String status) {
        BolsaSangue b = new BolsaSangue();
        b.setCodigo(repo.gerarCodigoBolsa());
        b.setTipoSanguineo(tipo);
        b.setComponente("Concentrado de Hemácias");
        b.setVolume(450);
        b.setStatus(status);
        b.setDataColeta(LocalDate.now().minusDays(2));
        b.setDataValidade(LocalDate.now().plusDays(30));
        repo.getEstoque().inserirOrdenado(b, Comparator.comparing(BolsaSangue::getDataValidade));
        return b;
    }

    // =========================================================================
    // HU11 — Agendamento de Doação
    // =========================================================================

    @Test
    void hu11_cenario1_agendamentoComDoadorAptoEHorarioDisponivel() {
        Doador maria = criarDoador("Maria Silva", "11122233344", "F", "O+", 60.0, null);
        LocalDate dataDesejada = LocalDate.now().plusDays(5);

        String erro = agendamentoService.agendar(maria.getId(), dataDesejada, "09:00");
        assertNull(erro, "Agendamento deveria ter sido aceito");

        List<Agendamento> agendamentos = agendamentoService.listar();
        assertEquals(1, agendamentos.size());
        assertEquals("agendado", agendamentos.get(0).getStatus());
        assertEquals("09:00", agendamentos.get(0).getHorario());
        assertEquals(dataDesejada, agendamentos.get(0).getData());
    }

    @Test
    void hu11_cenario2_tentativaAgendamentoEmHorarioSemVagas() {
        repo.getConfiguracao().setLimiteVagasPorHorario(2);
        LocalDate dia = LocalDate.now().plusDays(3);

        Doador d1 = criarDoador("Doador 1", "00100200301", "M", "A+", 70.0, null);
        Doador d2 = criarDoador("Doador 2", "00100200302", "F", "B+", 65.0, null);
        Doador d3 = criarDoador("Doador 3", "00100200303", "M", "O+", 80.0, null);

        assertNull(agendamentoService.agendar(d1.getId(), dia, "09:00"));
        assertNull(agendamentoService.agendar(d2.getId(), dia, "09:00"));

        // Terceiro agendamento no mesmo horário esgota o limite
        String erro = agendamentoService.agendar(d3.getId(), dia, "09:00");
        assertEquals("Horário indisponível", erro);

        long ativos = agendamentoService.listar().stream()
                .filter(a -> "agendado".equals(a.getStatus()) && a.getHorario().equals("09:00"))
                .count();
        assertEquals(2, ativos);
    }

    @Test
    void hu11_cenario3_tentativaAgendamentoAntesDoIntervaloMinimo() {
        // João Souza doou há 30 dias (homem exige 60 dias)
        Doador joao = criarDoador("João Souza", "12345678909", "M", "A+", 75.0, LocalDate.now().minusDays(30));

        // Tenta agendar para amanhã (31 dias após última doação)
        String erro = agendamentoService.agendar(joao.getId(), LocalDate.now().plusDays(1), "10:00");
        assertEquals("Doador ainda não apto para nova doação", erro);

        assertTrue(agendamentoService.listar().isEmpty());
    }

    @Test
    void hu11_cenario4_cancelamentoDeAgendamentoLiberaVaga() {
        repo.getConfiguracao().setLimiteVagasPorHorario(1);
        LocalDate dia = LocalDate.now().plusDays(2);

        Doador d1 = criarDoador("Doador 1", "00100200301", "M", "A+", 70.0, null);
        Doador d2 = criarDoador("Doador 2", "00100200302", "F", "B+", 65.0, null);

        assertNull(agendamentoService.agendar(d1.getId(), dia, "14:00"));

        Agendamento ag = agendamentoService.listar().get(0);
        assertEquals("agendado", ag.getStatus());

        // Cancelar agendamento
        String erroCancelamento = agendamentoService.cancelar(ag.getId());
        assertNull(erroCancelamento);
        assertEquals("cancelado", ag.getStatus());

        // Agora d2 consegue agendar no mesmo horário
        String erroNovo = agendamentoService.agendar(d2.getId(), dia, "14:00");
        assertNull(erroNovo, "Vaga deveria estar disponível após cancelamento");
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

        // Doadora liberada para registro de coleta
        String erroColeta = estoqueService.registrarColeta(maria.getId(), "Concentrado de Hemácias", 450);
        assertNull(erroColeta, "Coleta deveria ter sido registrada com sucesso");
    }

    @Test
    void hu12_cenario2_triagemComHemoglobinaAbaixoDoMinimo() {
        // Ana Lima (mulher: mínimo 12.5) com 11.8
        Doador ana = criarDoador("Ana Lima", "99988877766", "F", "A+", 58.0, null);

        TriagemClinica triagem = triagemService.registrarTriagem(ana.getId(), 58.0, "110/70", 36.6, 11.8);
        assertEquals("inapto", triagem.getResultado());
        assertEquals("Hemoglobina abaixo do mínimo", triagem.getMotivoInaptidao());
    }

    @Test
    void hu12_cenario3_tentativaDeColetaComDoadorInapto() {
        Doador joao = criarDoador("João Souza", "12345678909", "M", "O+", 72.0, null);

        // Homem com hemoglobina 12.0 (< 13.0) -> inapto
        triagemService.registrarTriagem(joao.getId(), 72.0, "130/85", 36.8, 12.0);

        String erroColeta = estoqueService.registrarColeta(joao.getId(), "Concentrado de Hemácias", 450);
        assertEquals("Doador inapto na triagem", erroColeta);
    }

    // =========================================================================
    // HU13 — Aprovação e Recusa de Solicitações de Sangue
    // =========================================================================

    @Test
    void hu13_cenario1_aprovacaoDeSolicitacaoComEstoqueSuficiente() {
        Hospital h = new Hospital();
        h.setId("HOSP-1");
        h.setCnpj("12345678000199");
        h.setRazaoSocial("Hospital Teste");
        h.setAtivo(true);
        repo.getHospitais().add(h);

        // 5 bolsas O- disponíveis
        for (int i = 0; i < 5; i++) {
            criarBolsa("O-", "disponivel");
        }

        requisicaoService.criarRequisicao("12345678000199", "O-", 3, false);
        Requisicao req = requisicaoService.listarFila().get(0);
        assertEquals("pendente", req.getStatus());

        String erro = requisicaoService.aprovarRequisicao(req.getId());
        assertNull(erro);
        assertEquals("aprovada", req.getStatus());

        long reservadas = repo.getEstoque().paraLista().stream()
                .filter(b -> "reservada".equals(b.getStatus()) && req.getId().equals(b.getRequisicaoId()))
                .count();
        assertEquals(3, reservadas, "3 bolsas devem estar reservadas para a requisição");
    }

    @Test
    void hu13_cenario2_aprovacaoComEstoqueInsuficiente() {
        Hospital h = new Hospital();
        h.setId("HOSP-2");
        h.setCnpj("98765432000199");
        h.setRazaoSocial("Hospital Teste 2");
        h.setAtivo(true);
        repo.getHospitais().add(h);

        // Apenas 2 bolsas O- disponíveis
        criarBolsa("O-", "disponivel");
        criarBolsa("O-", "disponivel");

        // Solicitação de 3 bolsas
        requisicaoService.criarRequisicao("98765432000199", "O-", 3, false);
        Requisicao req = requisicaoService.listarFila().get(0);

        String erro = requisicaoService.aprovarRequisicao(req.getId());
        assertEquals("Estoque insuficiente para atender a solicitação", erro);
        assertEquals("pendente", req.getStatus());
    }

    @Test
    void hu13_cenario3_recusaDeSolicitacaoComMotivo() {
        Hospital h = new Hospital();
        h.setId("HOSP-3");
        h.setCnpj("55544433000199");
        h.setRazaoSocial("Hospital Teste 3");
        h.setAtivo(true);
        repo.getHospitais().add(h);

        requisicaoService.criarRequisicao("55544433000199", "A+", 2, false);
        Requisicao req = requisicaoService.listarFila().get(0);
        assertEquals("pendente", req.getStatus());

        String motivo = "Documentação incompleta do hospital solicitante";
        String erro = requisicaoService.recusarRequisicao(req.getId(), motivo);
        assertNull(erro);

        assertEquals("recusada", req.getStatus());
        assertEquals(motivo, req.getMotivoRecusa());

        // Verificar histórico registrado
        HistoricoOperacao hist = repo.getHistoricoStack().paraLista().stream()
                .filter(op -> "RECUSA".equals(op.getTipo()))
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

        // Cria bolsas registradas para João
        BolsaSangue b = new BolsaSangue();
        b.setCodigo("BS-00099");
        b.setDoadorId(joao.getId());
        b.setTipoSanguineo("O+");
        b.setVolume(450);
        b.setComponente("Concentrado de Hemácias");
        b.setDataColeta(trintaDiasAtras);
        b.setDataValidade(trintaDiasAtras.plusDays(42));
        b.setStatus("distribuida");
        repo.getEstoque().inserirOrdenado(b, Comparator.comparing(BolsaSangue::getDataValidade));

        List<BolsaSangue> doacoes = doadorService.listarDoacoesDoador(joao.getId());
        assertEquals(1, doacoes.size());
        assertEquals(trintaDiasAtras, doacoes.get(0).getDataColeta());
        assertEquals("O+", doacoes.get(0).getTipoSanguineo());
        assertEquals(450, doacoes.get(0).getVolume());

        LocalDate proximaData = doadorService.calcularProximaDataApta(joao);
        assertEquals(trintaDiasAtras.plusDays(60), proximaData, "Homem deve aguardar 60 dias");
        assertFalse(doadorService.estaApto(joao), "Com 30 dias desde a última doação, ainda não está apto");
    }

    @Test
    void hu14_cenario2_consultaDeDoadorSemDoacoesAnteriores() {
        Doador carlos = criarDoador("Carlos Pereira", "99900011122", "M", "AB+", 82.0, null);

        List<BolsaSangue> doacoes = doadorService.listarDoacoesDoador(carlos.getId());
        assertTrue(doacoes.isEmpty(), "Não deve haver doações registradas");

        assertTrue(doadorService.estaApto(carlos), "Doador sem doações anteriores deve aparecer como apto");
    }

    // =========================================================================
    // Testes de Integração Web (Controllers & Templates via MockMvc)
    // =========================================================================

    @Test
    void web_hu11_rotasDeAgendamento() throws Exception {
        Doador d = criarDoador("Lucas Mendes", "55566677788", "M", "O+", 75.0, null);

        mockMvc.perform(get("/agendamentos"))
                .andExpect(status().isOk())
                .andExpect(view().name("agendamentos/lista"));

        mockMvc.perform(get("/agendamentos/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("agendamentos/form"));

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

        mockMvc.perform(get("/triagem"))
                .andExpect(status().isOk())
                .andExpect(view().name("triagem/lista"));

        mockMvc.perform(get("/triagem/nova"))
                .andExpect(status().isOk())
                .andExpect(view().name("triagem/form"));

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
        Hospital h = new Hospital();
        h.setId("HOSP-WEB");
        h.setCnpj("77788899000100");
        h.setRazaoSocial("Hospital Web");
        h.setAtivo(true);
        repo.getHospitais().add(h);

        for (int i = 0; i < 3; i++) {
            criarBolsa("A+", "disponivel");
        }

        requisicaoService.criarRequisicao("77788899000100", "A+", 2, false);
        Requisicao req = requisicaoService.listarFila().get(0);

        // Testar aprovação via POST
        mockMvc.perform(post("/requisicoes/aprovar/" + req.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requisicoes"))
                .andExpect(flash().attributeExists("sucesso"));

        // Criar outra requisição para testar recusa via POST
        requisicaoService.criarRequisicao("77788899000100", "A+", 1, false);
        Requisicao req2 = requisicaoService.listarFila().get(1);

        mockMvc.perform(post("/requisicoes/recusar/" + req2.getId())
                        .param("motivo", "Falta de transporte no hospital"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requisicoes"))
                .andExpect(flash().attributeExists("sucesso"));
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
