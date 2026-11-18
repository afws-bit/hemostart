package br.com.rotavital.controller.api;

import br.com.rotavital.dto.api.*;
import br.com.rotavital.service.RequisicaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Contrato de API — solicitações e distribuições (HU05, HU06, HU13). */
@RestController
@RequestMapping("/api/v1")
public class SolicitacaoApiController {

    private final RequisicaoService requisicaoService;

    public SolicitacaoApiController(RequisicaoService requisicaoService) {
        this.requisicaoService = requisicaoService;
    }

    /** 201 · 400 · 403 hospital não cadastrado ou inativo. */
    @PostMapping("/solicitacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitacaoResponse criar(@Valid @RequestBody SolicitacaoRequest request) {
        return SolicitacaoResponse.de(requisicaoService.criarRequisicao(
                request.cnpjHospital(), request.tipoSanguineo(), request.quantidade(), request.urgente()));
    }

    /** 200 — fila de atendimento (urgentes primeiro). */
    @GetMapping("/solicitacoes")
    public List<SolicitacaoResponse> listar() {
        return requisicaoService.listarFila().stream().map(SolicitacaoResponse::de).toList();
    }

    /** 200 · 404. */
    @GetMapping("/solicitacoes/{id}")
    public SolicitacaoResponse buscar(@PathVariable("id") String id) {
        return SolicitacaoResponse.de(requisicaoService.buscarPorId(id));
    }

    /** 200 · 404 · 409 (não pendente ou estoque insuficiente) — reserva bolsas e calcula a rota. */
    @PostMapping("/solicitacoes/{id}/aprovacao")
    public SolicitacaoResponse aprovar(@PathVariable("id") String id) {
        return SolicitacaoResponse.de(requisicaoService.aprovarRequisicao(id));
    }

    /** 200 · 400 · 404 · 409. */
    @PostMapping("/solicitacoes/{id}/recusa")
    public SolicitacaoResponse recusar(@PathVariable("id") String id, @Valid @RequestBody RecusaRequest request) {
        return SolicitacaoResponse.de(requisicaoService.recusarRequisicao(id, request.motivo()));
    }

    /** 201 · 400 · 404 · 409 (solicitação não aprovada ou bolsa já distribuída). */
    @PostMapping("/distribuicoes")
    @ResponseStatus(HttpStatus.CREATED)
    public DistribuicaoResponse distribuir(@Valid @RequestBody DistribuicaoRequest request) {
        List<BolsaResponse> bolsas = requisicaoService
                .confirmarDistribuicao(request.solicitacaoId(), request.responsavel())
                .stream().map(BolsaResponse::de).toList();
        return new DistribuicaoResponse(SolicitacaoResponse.de(requisicaoService.buscarPorId(request.solicitacaoId())), bolsas);
    }
}
