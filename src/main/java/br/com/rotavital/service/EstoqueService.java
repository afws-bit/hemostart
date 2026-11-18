package br.com.rotavital.service;

import br.com.rotavital.concorrencia.SecaoCritica;
import br.com.rotavital.dto.EstatisticaDesperdicioDTO;
import br.com.rotavital.dto.EstatisticasGeraisDTO;
import br.com.rotavital.estruturas.ListaEncadeada;
import br.com.rotavital.exception.RecursoNaoEncontradoException;
import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Componente;
import br.com.rotavital.model.Doador;
import br.com.rotavital.model.StatusBolsa;
import br.com.rotavital.model.TipoOperacao;
import br.com.rotavital.repository.BolsaSangueRepository;
import br.com.rotavital.repository.DoadorRepository;
import br.com.rotavital.repository.TriagemClinicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EstoqueService {

    private static final Comparator<BolsaSangue> POR_VALIDADE = Comparator.comparing(BolsaSangue::getDataValidade);

    private final BolsaSangueRepository bolsaRepository;
    private final DoadorRepository doadorRepository;
    private final HistoricoService historicoService;
    private final TriagemClinicaRepository triagemRepository;
    private final SecaoCritica secaoCritica;

    public EstoqueService(BolsaSangueRepository bolsaRepository,
                          DoadorRepository doadorRepository,
                          HistoricoService historicoService,
                          TriagemClinicaRepository triagemRepository,
                          SecaoCritica secaoCritica) {
        this.bolsaRepository = bolsaRepository;
        this.doadorRepository = doadorRepository;
        this.historicoService = historicoService;
        this.triagemRepository = triagemRepository;
        this.secaoCritica = secaoCritica;
    }

    /**
     * HU02 — registra a coleta, gera o código único e grava a bolsa no banco.
     * Roda na SecaoCritica: o código BS-xxxxx só é liberado para a próxima
     * coleta depois que esta foi gravada (sem códigos duplicados).
     */
    public BolsaSangue registrarColeta(String doadorId, String componente, int volume) {
        return secaoCritica.executar(() -> registrarColetaExclusivo(doadorId, componente, volume));
    }

    private BolsaSangue registrarColetaExclusivo(String doadorId, String componente, int volume) {
        Doador doador = doadorRepository.findById(doadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doador não encontrado"));
        LocalDate hoje = LocalDate.now();
        if (!doador.aptoPorIntervalo(hoje)) {
            throw new RegraNegocioException("Doador ainda não apto para nova doação");
        }
        // HU12 — a triagem mais recente bloqueia a coleta se o doador saiu inapto
        triagemRepository.findTopByDoador_IdOrderByDataHoraDesc(doadorId).ifPresent(t -> {
            if (!t.isApto()) throw new RegraNegocioException("Doador inapto na triagem");
        });
        if (volume <= 0) {
            throw new RegraNegocioException("Volume deve ser maior que zero");
        }

        BolsaSangue bolsa = BolsaSangue.coletar(gerarCodigoBolsa(), doador,
                Componente.deDescricao(componente), volume, hoje);
        bolsaRepository.save(bolsa);

        doador.registrarDoacao(hoje);
        doadorRepository.save(doador);

        historicoService.registrar(TipoOperacao.COLETA, bolsa.getCodigo(),
                "Coleta registrada para " + doador.getNome() + " - " + bolsa.getComponente());
        return bolsa;
    }

    /** Próximo código sequencial BS-00001, BS-00002... a partir do maior já gravado. */
    public String gerarCodigoBolsa() {
        int ultimo = bolsaRepository.findTopByOrderByCodigoDesc()
                .map(BolsaSangue::getCodigo)
                .filter(c -> c.startsWith("BS-"))
                .map(c -> Integer.parseInt(c.substring(3)))
                .orElse(0);
        return String.format("BS-%05d", ultimo + 1);
    }

    /** Lê todas as bolsas do banco e as organiza em uma ListaEncadeada ordenada por validade (FEFO). */
    @Transactional(readOnly = true)
    public List<BolsaSangue> listarEstoque() {
        ListaEncadeada<BolsaSangue> lista = new ListaEncadeada<>();
        bolsaRepository.findAll().forEach(b -> lista.inserirOrdenado(b, POR_VALIDADE));
        return lista.paraLista();
    }

    @Transactional(readOnly = true)
    public BolsaSangue buscarPorCodigo(String codigo) {
        return bolsaRepository.findById(codigo).orElse(null);
    }

    /** HU03 / HU07 — verificação periódica de validade (chamada pelo ValidadeScheduler). */
    public void verificarValidade() {
        secaoCritica.executar(this::verificarValidadeExclusivo);
    }

    private void verificarValidadeExclusivo() {
        LocalDate hoje = LocalDate.now();
        List<BolsaSangue> ativas = bolsaRepository.findByStatusInOrderByDataValidadeAsc(
                EnumSet.of(StatusBolsa.DISPONIVEL, StatusBolsa.RESERVADA));
        for (BolsaSangue b : ativas) {
            if (b.atualizarValidade(hoje)) {
                historicoService.registrar(TipoOperacao.VENCIMENTO, b.getCodigo(),
                        "Bolsa vencida em " + b.getDataValidade());
            }
        }
        bolsaRepository.saveAll(ativas);
    }

    /** HU07 — descarte confirmado pelo responsável técnico. */
    public BolsaSangue descartar(String codigo, String responsavel) {
        if (responsavel == null || responsavel.isBlank()) {
            throw new RegraNegocioException("Informe o responsável pelo descarte");
        }
        return secaoCritica.executar(() -> {
            BolsaSangue b = bolsaRepository.findById(codigo)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Bolsa não encontrada"));
            b.descartar(responsavel);
            historicoService.registrar(TipoOperacao.DESCARTE, b.getCodigo(), "Descartada por " + responsavel);
            return bolsaRepository.save(b);
        });
    }

    @Transactional(readOnly = true)
    public List<BolsaSangue> bolsasVencidas() {
        return bolsaRepository.findByStatusOrderByDataValidadeAsc(StatusBolsa.VENCIDA);
    }

    @Transactional(readOnly = true)
    public List<BolsaSangue> bolsasUsoPrioritario() {
        return bolsaRepository.findByStatusAndUsoPrioritarioTrueOrderByDataValidadeAsc(StatusBolsa.DISPONIVEL);
    }

    // =====================================================================
    // MÉTODOS DE ESTATÍSTICA (EST)
    // =====================================================================

    private boolean isDesperdicio(BolsaSangue b) {
        return b.getStatus() == StatusBolsa.DESCARTADA || b.getStatus() == StatusBolsa.VENCIDA;
    }

    /** Semanas analisadas na estatística de desperdício. */
    public static final int SEMANAS_DESPERDICIO = 8;

    /**
     * EST — desperdício por semana nas últimas 8 semanas (bolsas descartadas ou vencidas).
     * A amostra é a quantidade desperdiçada em cada semana, o que dá média, mediana
     * e desvio padrão com significado (antes cada bolsa valia 1, e o desvio era sempre 0).
     * A data usada é a do descarte ou, para bolsas vencidas, a da validade.
     */
    @Transactional(readOnly = true)
    public EstatisticaDesperdicioDTO calcularEstatisticasDesperdicio() {
        List<BolsaSangue> descartadas = bolsaRepository.findAll().stream()
                .filter(this::isDesperdicio)
                .collect(Collectors.toList());

        Map<String, Long> descartePorTipo = descartadas.stream()
                .collect(Collectors.groupingBy(BolsaSangue::getTipoSanguineo, Collectors.counting()));

        LocalDate hoje = LocalDate.now();
        double[] porSemana = new double[SEMANAS_DESPERDICIO];
        for (BolsaSangue b : descartadas) {
            LocalDate data = b.getDataDescarte() != null ? b.getDataDescarte().toLocalDate() : b.getDataValidade();
            if (data == null || data.isAfter(hoje)) continue;
            long semana = ChronoUnit.DAYS.between(data, hoje) / 7;
            if (semana < SEMANAS_DESPERDICIO) porSemana[(int) semana]++;
        }

        List<Double> valores = new java.util.ArrayList<>();
        for (double v : porSemana) valores.add(v);
        valores.sort(Comparator.naturalOrder());

        int n = valores.size();
        double media = valores.stream().mapToDouble(Double::doubleValue).sum() / n;
        double mediana = (n % 2 == 1)
                ? valores.get(n / 2)
                : (valores.get(n / 2 - 1) + valores.get(n / 2)) / 2.0;
        double variancia = valores.stream().mapToDouble(v -> Math.pow(v - media, 2)).sum() / (n - 1); // amostral
        double desvioPadrao = Math.sqrt(variancia);

        return new EstatisticaDesperdicioDTO(media, mediana, desvioPadrao, descartadas.size(),
                descartePorTipo.isEmpty() ? new HashMap<>() : descartePorTipo);
    }

    @Transactional(readOnly = true)
    public EstatisticasGeraisDTO calcularEstatisticasGerais() {
        List<BolsaSangue> todas = bolsaRepository.findAll();
        if (todas.isEmpty()) {
            return new EstatisticasGeraisDTO(new HashMap<>(), 0, 0);
        }

        Map<String, Double> bolsasPorTipo = new HashMap<>();
        todas.stream()
                .filter(BolsaSangue::isDisponivel)
                .collect(Collectors.groupingBy(BolsaSangue::getTipoSanguineo, Collectors.counting()))
                .forEach((tipo, qtd) -> bolsasPorTipo.put(tipo, qtd.doubleValue()));

        long totalDesperdicio = todas.stream().filter(this::isDesperdicio).count();
        double taxaDesperdicio = (double) totalDesperdicio / todas.size() * 100.0;

        double tempoMedio = todas.stream()
                .filter(b -> b.getDataColeta() != null && b.getDataValidade() != null)
                .mapToLong(b -> ChronoUnit.DAYS.between(b.getDataColeta(), b.getDataValidade()))
                .average()
                .orElse(0.0);

        return new EstatisticasGeraisDTO(bolsasPorTipo, taxaDesperdicio, tempoMedio);
    }
}
