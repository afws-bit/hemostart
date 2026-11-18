package br.com.rotavital.service;

import br.com.rotavital.model.*;
import br.com.rotavital.repository.JsonDataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RequisicaoService {

    private final JsonDataRepository repo;
    private final HospitalService hospitalService;
    private final CompatibilidadeService compatibilidadeService;

    public RequisicaoService(JsonDataRepository repo,
                              HospitalService hospitalService,
                              CompatibilidadeService compatibilidadeService) {
        this.repo = repo;
        this.hospitalService = hospitalService;
        this.compatibilidadeService = compatibilidadeService;
    }

    public String criarRequisicao(String cnpjHospital, String tipoSanguineo,
                                   int quantidade, boolean urgente) {
        Hospital hospital = hospitalService.buscarPorCnpj(cnpjHospital);
        if (hospital == null || !hospital.isAtivo()) {
            return "Hospital não autorizado";
        }

        Requisicao req = new Requisicao();
        req.setId(UUID.randomUUID().toString());
        req.setHospitalId(hospital.getId());
        req.setTipoSanguineo(tipoSanguineo);
        req.setQuantidade(quantidade);
        req.setUrgente(urgente);
        req.setStatus("pendente");
        req.setDataSolicitacao(LocalDateTime.now());

        repo.getFilaRequisicoes().enfileirar(req);
        repo.getTodasRequisicoes().add(req);
        repo.gravar();
        return null;
    }

    public List<Requisicao> listarFila() {
        return repo.getFilaRequisicoes().paraLista();
    }

    public Requisicao buscarPorId(String id) {
        return repo.getFilaRequisicoes().paraLista().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseGet(() -> repo.getTodasRequisicoes().stream()
                        .filter(r -> r.getId().equals(id))
                        .findFirst().orElse(null));
    }

    public String aprovarRequisicao(String requisicaoId) {
        repo.getLock().lock();
        try {
            Requisicao req = buscarPorId(requisicaoId);
            if (req == null) return "Requisição não encontrada";
            if (!"pendente".equals(req.getStatus())) {
                return "Apenas solicitações com status 'pendente' podem ser aprovadas ou recusadas";
            }

            List<BolsaSangue> sugeridas = compatibilidadeService
                    .sugerirBolsas(req.getTipoSanguineo(), req.getQuantidade());

            if (sugeridas.size() < req.getQuantidade()) {
                return "Estoque insuficiente para atender a solicitação";
            }

            for (BolsaSangue b : sugeridas) {
                b.setStatus("reservada");
                b.setRequisicaoId(req.getId());
                HistoricoOperacao h = new HistoricoOperacao(
                        "RESERVA", b.getCodigo(),
                        "Reservada para requisição " + req.getId()
                );
                repo.getHistoricoStack().empilhar(h);
            }
            req.setStatus("aprovada");
            repo.gravar();
            return null;
        } finally {
            repo.getLock().unlock();
        }
    }

    public String recusarRequisicao(String requisicaoId, String motivo) {
        repo.getLock().lock();
        try {
            Requisicao req = buscarPorId(requisicaoId);
            if (req == null) return "Requisição não encontrada";
            if (!"pendente".equals(req.getStatus())) {
                return "Apenas solicitações com status 'pendente' podem ser aprovadas ou recusadas";
            }
            if (motivo == null || motivo.trim().isEmpty()) {
                return "Motivo da recusa é obrigatório";
            }

            req.setStatus("recusada");
            req.setMotivoRecusa(motivo.trim());

            HistoricoOperacao h = new HistoricoOperacao(
                    "RECUSA", null,
                    "Requisição " + req.getId() + " recusada. Motivo: " + motivo.trim()
            );
            repo.getHistoricoStack().empilhar(h);
            repo.gravar();
            return null;
        } finally {
            repo.getLock().unlock();
        }
    }

    public String reservarBolsasParaPrimeiro() {
        repo.getLock().lock();
        try {
            Requisicao req = repo.getFilaRequisicoes().paraLista().stream()
                    .filter(r -> "pendente".equals(r.getStatus()))
                    .findFirst().orElse(null);
            if (req == null) return "Fila vazia";
            return aprovarRequisicao(req.getId());
        } finally {
            repo.getLock().unlock();
        }
    }

    public String confirmarDistribuicao(String requisicaoId, String responsavel) {
        repo.getLock().lock();
        try {
            Requisicao req = repo.getFilaRequisicoes().paraLista().stream()
                    .filter(r -> r.getId().equals(requisicaoId))
                    .findFirst().orElse(null);
            if (req == null) return "Requisição não encontrada na fila";

            List<BolsaSangue> bolsasReservadas = repo.getEstoque().paraLista().stream()
                    .filter(b -> requisicaoId.equals(b.getRequisicaoId()) && "reservada".equals(b.getStatus()))
                    .toList();

            for (BolsaSangue b : bolsasReservadas) {
                if ("distribuida".equals(b.getStatus())) {
                    return "Bolsa já distribuída anteriormente";
                }
                b.setStatus("distribuida");
                b.setDataDistribuicao(LocalDateTime.now());
                b.setResponsavelDistribuicao(responsavel);
                b.setHospitalDestinoId(req.getHospitalId());
                HistoricoOperacao h = new HistoricoOperacao(
                        "DISTRIBUICAO", b.getCodigo(),
                        "Distribuída por " + responsavel + " para hospital " + req.getHospitalId()
                );
                repo.getHistoricoStack().empilhar(h);
            }

            req.setStatus("atendida");
            req.setDataAtendimento(LocalDateTime.now());
            req.setResponsavelDistribuicao(responsavel);
            repo.getFilaRequisicoes().remover(requisicaoId);
            repo.gravar();
            return null;
        } finally {
            repo.getLock().unlock();
        }
    }

    public String distribuirPorCodigo(String codigoBolsa, String responsavel) {
        repo.getLock().lock();
        try {
            BolsaSangue b = repo.getEstoque().buscar(bolsa -> bolsa.getCodigo().equals(codigoBolsa));
            if (b == null) return "Bolsa não encontrada";
            if ("distribuida".equals(b.getStatus())) return "Bolsa já distribuída anteriormente";

            b.setStatus("distribuida");
            b.setDataDistribuicao(LocalDateTime.now());
            b.setResponsavelDistribuicao(responsavel);
            HistoricoOperacao h = new HistoricoOperacao(
                    "DISTRIBUICAO", b.getCodigo(),
                    "Distribuída diretamente por " + responsavel
            );
            repo.getHistoricoStack().empilhar(h);
            repo.gravar();
            return null;
        } finally {
            repo.getLock().unlock();
        }
    }
}
