package br.com.rotavital.service;

import br.com.rotavital.aed.AlocadorBolsas;
import br.com.rotavital.aed.GrafoRotas;
import br.com.rotavital.aed.ResultadoAlocacao;
import br.com.rotavital.aed.TabelaHashEstoque;
import br.com.rotavital.config.MapaRotasConfig;
import br.com.rotavital.estruturas.FilaRequisicoes;
import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Hospital;
import br.com.rotavital.model.Requisicao;
import br.com.rotavital.model.StatusBolsa;
import br.com.rotavital.model.StatusRequisicao;
import br.com.rotavital.model.TipoOperacao;
import br.com.rotavital.model.TipoSanguineo;
import br.com.rotavital.repository.BolsaSangueRepository;
import br.com.rotavital.repository.RequisicaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

@Service
public class RequisicaoService {

    private final RequisicaoRepository requisicaoRepository;
    private final BolsaSangueRepository bolsaRepository;
    private final HospitalService hospitalService;
    private final CompatibilidadeService compatibilidadeService;
    private final HistoricoService historicoService;
    private final GrafoRotas mapaRotas;

    public RequisicaoService(RequisicaoRepository requisicaoRepository,
                             BolsaSangueRepository bolsaRepository,
                             HospitalService hospitalService,
                             CompatibilidadeService compatibilidadeService,
                             HistoricoService historicoService,
                             GrafoRotas mapaRotas) {
        this.requisicaoRepository = requisicaoRepository;
        this.bolsaRepository = bolsaRepository;
        this.hospitalService = hospitalService;
        this.compatibilidadeService = compatibilidadeService;
        this.historicoService = historicoService;
        this.mapaRotas = mapaRotas;
    }

    /** HU05 — só hospitais cadastrados e ativos podem solicitar. */
    @Transactional
    public Requisicao criarRequisicao(String cnpjHospital, String tipoSanguineo,
                                      int quantidade, boolean urgente) {
        Hospital hospital = hospitalService.buscarPorCnpj(cnpjHospital);
        if (hospital == null || !hospital.podeSolicitar()) {
            throw new RegraNegocioException("Hospital não autorizado");
        }
        if (quantidade <= 0) {
            throw new RegraNegocioException("Quantidade deve ser maior que zero");
        }
        try {
            TipoSanguineo.deRotulo(tipoSanguineo);
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Tipo sanguíneo inválido");
        }
        return requisicaoRepository.save(Requisicao.nova(hospital, tipoSanguineo, quantidade, urgente));
    }

    /**
     * Lê as requisições em aberto do banco (em ordem de chegada) e as coloca na
     * FilaRequisicoes, que posiciona as urgentes à frente das normais.
     */
    @Transactional(readOnly = true)
    public List<Requisicao> listarFila() {
        FilaRequisicoes fila = new FilaRequisicoes();
        requisicaoRepository
                .findByStatusInOrderByDataSolicitacaoAsc(EnumSet.of(StatusRequisicao.PENDENTE, StatusRequisicao.APROVADA, StatusRequisicao.RECUSADA))
                .forEach(fila::enfileirar);
        return fila.paraLista();
    }

    /**
     * HU04 + AED U2 — atende a primeira requisição pendente da fila usando os quatro
     * algoritmos integrados: carrega as bolsas disponíveis no índice por hash (cada tipo
     * com sua fila FEFO), aloca as compatíveis pela matriz ABO/Rh priorizando a validade
     * e calcula a rota hemocentro → hospital pelo vizinho mais próximo.
     */
    @Transactional
    public Requisicao reservarBolsasParaPrimeiro() {
        Requisicao req = listarFila().stream()
                .filter(Requisicao::isPendente)
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Fila vazia"));
        return aprovarRequisicao(req.getId());
    }

    @Transactional(readOnly = true)
    public Requisicao buscarPorId(String id) {
        return requisicaoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Requisição não encontrada"));
    }

    /** HU13 — aprova uma solicitação específica da fila (reserva via algoritmos da AED). */
    @Transactional
    public Requisicao aprovarRequisicao(String requisicaoId) {
        Requisicao req = buscarPorId(requisicaoId);
        if (!req.isPendente()) {
            throw new RegraNegocioException("Apenas solicitações com status 'pendente' podem ser aprovadas ou recusadas");
        }

        TabelaHashEstoque estoque = new TabelaHashEstoque();
        bolsaRepository.findByStatusOrderByDataValidadeAsc(StatusBolsa.DISPONIVEL).forEach(estoque::inserir);

        AlocadorBolsas alocador = new AlocadorBolsas(estoque, mapaRotas, MapaRotasConfig.HEMOCENTRO);
        ResultadoAlocacao resultado = alocador.alocar(
                req.getHospital().getRazaoSocial(), req.getTipoSanguineo(), req.getQuantidade());

        if (!resultado.atendido()) {
            throw new RegraNegocioException("Estoque insuficiente para atender a solicitação");
        }

        for (BolsaSangue b : resultado.bolsas()) {
            b.reservarPara(req);
            historicoService.registrar(TipoOperacao.RESERVA, b.getCodigo(),
                    "Reservada para " + req.getHospital().getRazaoSocial()
                            + " (paciente " + req.getTipoSanguineo() + ", rota: "
                            + resultado.rota().descricao() + ")");
        }
        bolsaRepository.saveAll(resultado.bolsas());
        req.aprovar();
        req.registrarRota(resultado.rota().descricao(),
                resultado.rota().encontrada() ? resultado.rota().distanciaTotal() : null);
        return requisicaoRepository.save(req);
    }

    /** HU13 — recusa uma solicitação pendente, com motivo obrigatório. */
    @Transactional
    public Requisicao recusarRequisicao(String requisicaoId, String motivo) {
        Requisicao req = buscarPorId(requisicaoId);
        if (!req.isPendente()) {
            throw new RegraNegocioException("Apenas solicitações com status 'pendente' podem ser aprovadas ou recusadas");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new RegraNegocioException("Motivo da recusa é obrigatório");
        }
        req.recusar(motivo.trim());
        historicoService.registrar(TipoOperacao.RECUSA, null,
                "Requisição " + req.getId() + " recusada. Motivo: " + motivo.trim());
        return requisicaoRepository.save(req);
    }

    /** Validação explícita usada pela tela/teste: reserva de bolsa específica para um tipo de paciente. */
    public void validarReserva(BolsaSangue bolsa, String tipoPaciente) {
        if (!compatibilidadeService.compativel(bolsa.getTipoSanguineo(), tipoPaciente)) {
            throw new RegraNegocioException("Tipo sanguíneo incompatível");
        }
    }

    /** HU06 — confirma a saída das bolsas reservadas para o hospital da requisição. */
    @Transactional
    public void confirmarDistribuicao(String requisicaoId, String responsavel) {
        exigirResponsavel(responsavel);
        Requisicao req = requisicaoRepository.findById(requisicaoId)
                .orElseThrow(() -> new RegraNegocioException("Requisição não encontrada na fila"));
        if (!req.isAprovada()) {
            throw new RegraNegocioException("A requisição precisa estar aprovada (com bolsas reservadas)");
        }

        List<BolsaSangue> reservadas = bolsaRepository.findByRequisicao_IdAndStatus(requisicaoId, StatusBolsa.RESERVADA);
        for (BolsaSangue b : reservadas) {
            b.distribuir(responsavel, req.getHospital());
            historicoService.registrar(TipoOperacao.DISTRIBUICAO, b.getCodigo(),
                    "Distribuída por " + responsavel + " para " + req.getHospital().getRazaoSocial());
        }
        bolsaRepository.saveAll(reservadas);

        req.atender(responsavel);
        requisicaoRepository.save(req);
    }

    /** HU06 — saída direta de uma bolsa pelo código (bloqueia distribuição duplicada). */
    @Transactional
    public void distribuirPorCodigo(String codigoBolsa, String responsavel) {
        exigirResponsavel(responsavel);
        BolsaSangue b = bolsaRepository.findById(codigoBolsa.trim())
                .orElseThrow(() -> new RegraNegocioException("Bolsa não encontrada"));
        Hospital destino = b.getRequisicao() != null ? b.getRequisicao().getHospital() : null;
        b.distribuir(responsavel, destino);
        bolsaRepository.save(b);
        historicoService.registrar(TipoOperacao.DISTRIBUICAO, b.getCodigo(),
                "Distribuída diretamente por " + responsavel);
    }

    private void exigirResponsavel(String responsavel) {
        if (responsavel == null || responsavel.isBlank()) {
            throw new RegraNegocioException("Informe o responsável pela distribuição");
        }
    }
}
