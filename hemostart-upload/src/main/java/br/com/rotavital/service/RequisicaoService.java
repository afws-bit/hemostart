package br.com.rotavital.service;

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

    public RequisicaoService(RequisicaoRepository requisicaoRepository,
                             BolsaSangueRepository bolsaRepository,
                             HospitalService hospitalService,
                             CompatibilidadeService compatibilidadeService,
                             HistoricoService historicoService) {
        this.requisicaoRepository = requisicaoRepository;
        this.bolsaRepository = bolsaRepository;
        this.hospitalService = hospitalService;
        this.compatibilidadeService = compatibilidadeService;
        this.historicoService = historicoService;
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
                .findByStatusInOrderByDataSolicitacaoAsc(EnumSet.of(StatusRequisicao.PENDENTE, StatusRequisicao.APROVADA))
                .forEach(fila::enfileirar);
        return fila.paraLista();
    }

    /** HU04 — reserva bolsas compatíveis (FEFO) para a primeira requisição pendente da fila. */
    @Transactional
    public Requisicao reservarBolsasParaPrimeiro() {
        Requisicao req = listarFila().stream()
                .filter(Requisicao::isPendente)
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Fila vazia"));

        List<BolsaSangue> sugeridas = compatibilidadeService
                .sugerirBolsas(req.getTipoSanguineo(), req.getQuantidade());

        if (sugeridas.size() < req.getQuantidade()) {
            throw new RegraNegocioException("Estoque insuficiente: apenas " + sugeridas.size()
                    + " bolsa(s) compatível(is) disponível(is)");
        }

        for (BolsaSangue b : sugeridas) {
            b.reservarPara(req);
            historicoService.registrar(TipoOperacao.RESERVA, b.getCodigo(),
                    "Reservada para " + req.getHospital().getRazaoSocial()
                            + " (paciente " + req.getTipoSanguineo() + ")");
        }
        bolsaRepository.saveAll(sugeridas);
        req.aprovar();
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
