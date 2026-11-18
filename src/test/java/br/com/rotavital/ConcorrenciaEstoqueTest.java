package br.com.rotavital;

import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.*;
import br.com.rotavital.repository.*;
import br.com.rotavital.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SO (PI3-82) — prova de AUSÊNCIA DE CONDIÇÃO DE CORRIDA.
 * Várias threads disparam a mesma operação ao mesmo tempo (todas esperam num
 * CountDownLatch e são liberadas juntas). Sem a SecaoCritica, estes testes
 * geravam códigos de bolsa repetidos, reservavam a mesma bolsa para dois
 * pedidos (issue #6 do GitHub) e ultrapassavam o limite de agendamentos.
 *
 * Esta classe NÃO usa @Transactional: cada thread precisa da sua própria
 * transação, como na aplicação real. Por isso os dados são apagados antes e depois.
 */
@SpringBootTest
class ConcorrenciaEstoqueTest {

    private static final int THREADS = 8;

    @Autowired private EstoqueService estoqueService;
    @Autowired private RequisicaoService requisicaoService;
    @Autowired private AgendamentoService agendamentoService;
    @Autowired private ConfiguracaoService configuracaoService;

    @Autowired private DoadorRepository doadorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private BolsaSangueRepository bolsaRepository;
    @Autowired private RequisicaoRepository requisicaoRepository;
    @Autowired private HistoricoOperacaoRepository historicoRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;
    @Autowired private TriagemClinicaRepository triagemRepository;

    @BeforeEach
    @AfterEach
    void limparBanco() {
        historicoRepository.deleteAll();
        bolsaRepository.deleteAll();
        agendamentoRepository.deleteAll();
        triagemRepository.deleteAll();
        requisicaoRepository.deleteAll();
        hospitalRepository.deleteAll();
        doadorRepository.deleteAll();
        configuracaoService.definirLimiteVagas(Configuracao.LIMITE_VAGAS_PADRAO);
    }

    private Doador doador(int i, String tipo) {
        Doador d = new Doador();
        d.setNome("Doador concorrente " + i);
        d.setCpf(String.format("5%010d", i));
        d.setDataNascimento(LocalDate.now().minusYears(30));
        d.setSexo("M");
        d.setTipoSanguineo(tipo);
        d.setPeso(80);
        return doadorRepository.save(d);
    }

    /** Executa as tarefas em paralelo, todas liberadas no mesmo instante. Devolve quantas deram certo. */
    private int executarJuntas(List<Callable<Object>> tarefas, List<Throwable> falhas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucessos = new AtomicInteger();
        List<Future<?>> futuros = new ArrayList<>();
        for (Callable<Object> tarefa : tarefas) {
            futuros.add(pool.submit(() -> {
                largada.await();
                try {
                    tarefa.call();
                    sucessos.incrementAndGet();
                } catch (Throwable t) {
                    synchronized (falhas) { falhas.add(t); }
                }
                return null;
            }));
        }
        largada.countDown();
        for (Future<?> f : futuros) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();
        return sucessos.get();
    }

    @Test
    void coletasSimultaneasGeramCodigosUnicos() throws Exception {
        List<Callable<Object>> tarefas = new ArrayList<>();
        for (int i = 0; i < THREADS * 2; i++) {
            Doador d = doador(i, "O+");
            tarefas.add(() -> estoqueService.registrarColeta(d.getId(), "Plasma", 300));
        }
        List<Throwable> falhas = new ArrayList<>();

        int sucessos = executarJuntas(tarefas, falhas);

        assertTrue(falhas.isEmpty(), "Nenhuma coleta deveria falhar: " + falhas);
        assertEquals(THREADS * 2, sucessos);
        List<BolsaSangue> bolsas = bolsaRepository.findAll();
        assertEquals(THREADS * 2, bolsas.size());
        assertEquals(THREADS * 2, new HashSet<>(bolsas.stream().map(BolsaSangue::getCodigo).toList()).size(),
                "Códigos de bolsa repetidos indicam condição de corrida");
    }

    @Test
    void aprovacoesSimultaneasNaoReservamAMesmaBolsa() throws Exception {
        Hospital h = new Hospital();
        h.setRazaoSocial("Hospital Concorrência");
        h.setCnpj("10203040000150");
        h.setAtivo(true);
        hospitalRepository.save(h);

        Doador d = doador(999, "O-");
        estoqueService.registrarColeta(d.getId(), "Concentrado de Hemácias", 450); // UMA bolsa O-

        List<Callable<Object>> tarefas = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Requisicao r = requisicaoService.criarRequisicao("10203040000150", "O-", 1, false);
            tarefas.add(() -> requisicaoService.aprovarRequisicao(r.getId()));
        }
        List<Throwable> falhas = new ArrayList<>();

        int sucessos = executarJuntas(tarefas, falhas);

        assertEquals(1, sucessos, "Só uma solicitação pode ficar com a única bolsa");
        assertEquals(THREADS - 1, falhas.size());
        assertTrue(falhas.stream().allMatch(t -> t instanceof RegraNegocioException
                && t.getMessage().equals("Estoque insuficiente para atender a solicitação")), "Falhas: " + falhas);
        assertEquals(1, requisicaoRepository.findAll().stream().filter(Requisicao::isAprovada).count());
        assertEquals(1, bolsaRepository.findByStatusOrderByDataValidadeAsc(StatusBolsa.RESERVADA).size());
    }

    @Test
    void agendamentosSimultaneosRespeitamOLimiteDeVagas() throws Exception {
        configuracaoService.definirLimiteVagas(2);
        LocalDate dia = LocalDate.now().plusDays(3);
        List<Callable<Object>> tarefas = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Doador d = doador(100 + i, "B+");
            tarefas.add(() -> agendamentoService.agendar(d.getId(), dia, "09:00"));
        }
        List<Throwable> falhas = new ArrayList<>();

        int sucessos = executarJuntas(tarefas, falhas);

        assertEquals(2, sucessos, "O limite é de 2 vagas por horário");
        assertEquals(2, agendamentoRepository.countByStatusAndDataAndHorario(Agendamento.AGENDADO, dia, "09:00"));
        assertTrue(falhas.stream().allMatch(t -> "Horário indisponível".equals(t.getMessage())), "Falhas: " + falhas);
    }
}
