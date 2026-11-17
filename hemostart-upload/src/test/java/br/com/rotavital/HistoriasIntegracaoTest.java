package br.com.rotavital;

import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.*;
import br.com.rotavital.repository.BolsaSangueRepository;
import br.com.rotavital.repository.DoadorRepository;
import br.com.rotavital.repository.HistoricoOperacaoRepository;
import br.com.rotavital.repository.RequisicaoRepository;
import br.com.rotavital.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração: cada teste exercita uma história de usuário
 * lendo e gravando no banco (H2 em memória durante os testes).
 * Cada método roda em uma transação que é desfeita ao final.
 */
@SpringBootTest
@Transactional
class HistoriasIntegracaoTest {

    @Autowired DoadorService doadorService;
    @Autowired HospitalService hospitalService;
    @Autowired EstoqueService estoqueService;
    @Autowired RequisicaoService requisicaoService;
    @Autowired CompatibilidadeService compatibilidadeService;
    @Autowired ConfiguracaoService configuracaoService;
    @Autowired HistoricoService historicoService;

    @Autowired DoadorRepository doadorRepository;
    @Autowired BolsaSangueRepository bolsaRepository;
    @Autowired RequisicaoRepository requisicaoRepository;
    @Autowired HistoricoOperacaoRepository historicoRepository;

    // ---------- helpers ----------

    private Doador novoDoador(String cpf, String tipo, String sexo) {
        Doador d = new Doador();
        d.setNome("Doador " + cpf);
        d.setCpf(cpf);
        d.setDataNascimento(LocalDate.now().minusYears(30));
        d.setSexo(sexo);
        d.setTipoSanguineo(tipo);
        d.setPeso(70);
        d.setContato("(81) 99999-0000");
        return d;
    }

    private Hospital novoHospital(String cnpj) {
        Hospital h = new Hospital();
        h.setRazaoSocial("Hospital " + cnpj);
        h.setCnpj(cnpj);
        h.setEndereco("Recife/PE");
        h.setContato("(81) 3333-0000");
        return hospitalService.cadastrar(h);
    }

    private BolsaSangue coletar(String cpf, String tipo) {
        Doador d = doadorService.cadastrar(novoDoador(cpf, tipo, "M"));
        return estoqueService.registrarColeta(d.getId(), Componente.CONCENTRADO_HEMACIAS.getDescricao(), 450);
    }

    // ---------- HU01 ----------

    @Test
    void hu01_cadastroValidoGravaNoBanco() {
        Doador salvo = doadorService.cadastrar(novoDoador("123.456.789-00", "O-", "F"));
        assertNotNull(salvo.getId());
        assertEquals("12345678900", doadorRepository.findById(salvo.getId()).orElseThrow().getCpf());
    }

    @Test
    void hu01_cpfDuplicadoNaoESalvo() {
        doadorService.cadastrar(novoDoador("12345678900", "O-", "F"));
        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> doadorService.cadastrar(novoDoador("123.456.789-00", "A+", "M")));
        assertEquals("CPF já cadastrado", e.getMessage());
        assertEquals(1, doadorRepository.count());
    }

    @Test
    void hu01_pesoAbaixoDoMinimo() {
        Doador d = novoDoador("11111111111", "A+", "M");
        d.setPeso(45);
        RegraNegocioException e = assertThrows(RegraNegocioException.class, () -> doadorService.cadastrar(d));
        assertEquals("Peso inferior ao mínimo permitido para doação", e.getMessage());
    }

    // ---------- HU02 ----------

    @Test
    void hu02_coletaGeraCodigoUnicoEValidade() {
        BolsaSangue b1 = coletar("22222222222", "O-");
        BolsaSangue b2 = coletar("33333333333", "O-");
        assertNotEquals(b1.getCodigo(), b2.getCodigo());
        assertEquals(LocalDate.now().plusDays(42), b1.getDataValidade());
        assertEquals(2, bolsaRepository.countByTipoSanguineoAndStatus("O-", StatusBolsa.DISPONIVEL));
    }

    @Test
    void hu02_coletaAntesDoIntervaloMinimoEBloqueada() {
        Doador d = novoDoador("44444444444", "B+", "M");
        d.setUltimaDoacao(LocalDate.now().minusDays(30));
        Doador salvo = doadorService.cadastrar(d);
        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> estoqueService.registrarColeta(salvo.getId(), "Plasma", 300));
        assertEquals("Doador ainda não apto para nova doação", e.getMessage());
        assertEquals(0, bolsaRepository.count());
    }

    // ---------- HU03 / HU07 ----------

    @Test
    void hu03_verificacaoDiariaMarcaVencidasEPrioritarias() {
        BolsaSangue vencendo = coletar("55555555555", "B-");
        vencendo.setDataValidade(LocalDate.now());
        BolsaSangue quaseVencendo = coletar("66666666666", "AB+");
        quaseVencendo.setDataValidade(LocalDate.now().plusDays(4));
        bolsaRepository.saveAll(List.of(vencendo, quaseVencendo));

        estoqueService.verificarValidade();

        assertEquals(StatusBolsa.VENCIDA, bolsaRepository.findById(vencendo.getCodigo()).orElseThrow().getStatus());
        assertTrue(bolsaRepository.findById(quaseVencendo.getCodigo()).orElseThrow().isUsoPrioritario());
        assertEquals(0, bolsaRepository.countByTipoSanguineoAndStatus("B-", StatusBolsa.DISPONIVEL));
    }

    @Test
    void hu07_descarteDeBolsaVencida() {
        BolsaSangue b = coletar("77777777777", "O+");
        b.setDataValidade(LocalDate.now().minusDays(1));
        bolsaRepository.save(b);
        estoqueService.verificarValidade();

        estoqueService.descartar(b.getCodigo(), "Dra. Ana");

        BolsaSangue lida = bolsaRepository.findById(b.getCodigo()).orElseThrow();
        assertEquals(StatusBolsa.DESCARTADA, lida.getStatus());
        assertEquals("Dra. Ana", lida.getResponsavelDescarte());
        assertThrows(RegraNegocioException.class, () -> estoqueService.descartar(b.getCodigo(), "Dra. Ana"));
    }

    // ---------- HU04 ----------

    @Test
    void hu04_sugereBolsaCompativelComValidadeMaisProxima() {
        BolsaSangue maisNova = coletar("88888888888", "O-");
        BolsaSangue maisAntiga = coletar("99999999999", "O-");
        maisAntiga.setDataValidade(LocalDate.now().plusDays(10));
        bolsaRepository.save(maisAntiga);

        List<BolsaSangue> sugeridas = compatibilidadeService.sugerirBolsas("O-", 1);
        assertEquals(1, sugeridas.size());
        assertEquals(maisAntiga.getCodigo(), sugeridas.get(0).getCodigo());
        assertNotEquals(maisNova.getCodigo(), sugeridas.get(0).getCodigo());
    }

    @Test
    void hu04_reservaIncompativelEBloqueada() {
        BolsaSangue aPos = coletar("10101010101", "A+");
        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> requisicaoService.validarReserva(aPos, "B-"));
        assertEquals("Tipo sanguíneo incompatível", e.getMessage());
    }

    // ---------- HU05 / HU09 ----------

    @Test
    void hu05_solicitacaoUrgenteVaiParaOTopoDaFila() {
        novoHospital("12345678000195");
        requisicaoService.criarRequisicao("12345678000195", "A+", 1, false);
        requisicaoService.criarRequisicao("12.345.678/0001-95", "O-", 3, true);

        List<Requisicao> fila = requisicaoService.listarFila();
        assertEquals(2, fila.size());
        assertTrue(fila.get(0).isUrgente());
        assertEquals(StatusRequisicao.PENDENTE, fila.get(0).getStatus());
    }

    @Test
    void hu05_hospitalNaoCadastradoEInativoSaoRecusados() {
        assertThrows(RegraNegocioException.class,
                () -> requisicaoService.criarRequisicao("00000000000000", "O-", 1, false));

        Hospital h = novoHospital("98765432000188");
        hospitalService.alternarStatus(h.getId());
        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> requisicaoService.criarRequisicao("98765432000188", "O-", 1, false));
        assertEquals("Hospital não autorizado", e.getMessage());
        assertEquals(0, requisicaoRepository.count());
    }

    @Test
    void hu09_cnpjDuplicado() {
        novoHospital("11122233000177");
        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> novoHospital("11.122.233/0001-77"));
        assertEquals("CNPJ já cadastrado", e.getMessage());
    }

    // ---------- HU06 / HU08 ----------

    @Test
    void hu06_distribuicaoCompletaERastreio() {
        Hospital h = novoHospital("22233344000155");
        BolsaSangue b = coletar("12121212121", "O-");
        Requisicao req = requisicaoService.criarRequisicao(h.getCnpj(), "O-", 1, true);

        requisicaoService.reservarBolsasParaPrimeiro();
        assertEquals(StatusBolsa.RESERVADA, bolsaRepository.findById(b.getCodigo()).orElseThrow().getStatus());

        requisicaoService.confirmarDistribuicao(req.getId(), "Operador João");

        BolsaSangue lida = bolsaRepository.findById(b.getCodigo()).orElseThrow();
        assertEquals(StatusBolsa.DISTRIBUIDA, lida.getStatus());
        assertEquals(h.getId(), lida.getHospitalDestinoId());
        assertNotNull(lida.getDataDistribuicao());
        assertEquals(StatusRequisicao.ATENDIDA, requisicaoRepository.findById(req.getId()).orElseThrow().getStatus());

        List<HistoricoOperacao> eventos = historicoService.historicosDaBolsa(b.getCodigo());
        assertEquals(List.of(TipoOperacao.COLETA, TipoOperacao.RESERVA, TipoOperacao.DISTRIBUICAO),
                eventos.stream().map(HistoricoOperacao::getTipo).toList());

        RegraNegocioException e = assertThrows(RegraNegocioException.class,
                () -> requisicaoService.distribuirPorCodigo(b.getCodigo(), "Operador João"));
        assertEquals("Bolsa já distribuída anteriormente", e.getMessage());
    }

    @Test
    void hu08_relatorioPorPeriodoETipo() {
        coletar("13131313131", "A+");
        coletar("14141414141", "B+");
        Map<String, Object> rel = historicoService.gerarRelatorio(
                LocalDate.now().minusDays(1), LocalDate.now(), "A+", "TODOS");
        assertEquals(1L, rel.get("coletadas"));
        assertEquals(0L, rel.get("distribuidas"));
        assertEquals(false, rel.get("vazio"));
    }

    // ---------- HU10 ----------

    @Test
    void hu10_alertaDeEstoqueCriticoEApagadoAoNormalizar() {
        configuracaoService.definirMinimo("O-", 2);
        coletar("15151515151", "O-");

        @SuppressWarnings("unchecked")
        Map<String, Boolean> alertas = (Map<String, Boolean>) compatibilidadeService.gerarPainelEstoque().get("alertas");
        assertTrue(alertas.get("O-"));

        coletar("16161616161", "O-");
        @SuppressWarnings("unchecked")
        Map<String, Boolean> depois = (Map<String, Boolean>) compatibilidadeService.gerarPainelEstoque().get("alertas");
        assertFalse(depois.get("O-"));
    }
}
