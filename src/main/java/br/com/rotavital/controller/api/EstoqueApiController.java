package br.com.rotavital.controller.api;

import br.com.rotavital.aed.MatrizCompatibilidade;
import br.com.rotavital.dto.api.*;
import br.com.rotavital.exception.RecursoNaoEncontradoException;
import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Configuracao;
import br.com.rotavital.model.TipoSanguineo;
import br.com.rotavital.service.CompatibilidadeService;
import br.com.rotavital.service.ConfiguracaoService;
import br.com.rotavital.service.EstoqueService;
import br.com.rotavital.service.HistoricoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Contrato de API — coletas, estoque, bolsas e compatibilidade (HU02, HU03, HU04, HU06, HU07). */
@RestController
@RequestMapping("/api/v1")
public class EstoqueApiController {

    private final EstoqueService estoqueService;
    private final CompatibilidadeService compatibilidadeService;
    private final ConfiguracaoService configuracaoService;
    private final HistoricoService historicoService;

    public EstoqueApiController(EstoqueService estoqueService, CompatibilidadeService compatibilidadeService,
                                ConfiguracaoService configuracaoService, HistoricoService historicoService) {
        this.estoqueService = estoqueService;
        this.compatibilidadeService = compatibilidadeService;
        this.configuracaoService = configuracaoService;
        this.historicoService = historicoService;
    }

    /** 201 · 400 · 404 doador · 409 doador inapto (intervalo ou triagem). */
    @PostMapping("/coletas")
    @ResponseStatus(HttpStatus.CREATED)
    public BolsaResponse registrarColeta(@Valid @RequestBody ColetaRequest request) {
        return BolsaResponse.de(estoqueService.registrarColeta(request.doadorId(), request.componente(), request.volume()));
    }

    /** 200 — quantidade por tipo e status + alerta de estoque crítico. */
    @GetMapping("/estoque")
    @SuppressWarnings("unchecked")
    public List<EstoqueTipoResponse> estoque() {
        Map<String, Object> dados = compatibilidadeService.gerarPainelEstoque();
        Map<String, Map<String, Long>> painel = (Map<String, Map<String, Long>>) dados.get("painel");
        Map<String, Boolean> alertas = (Map<String, Boolean>) dados.get("alertas");
        Configuracao config = configuracaoService.obter();
        List<EstoqueTipoResponse> resposta = new ArrayList<>();
        for (String tipo : TipoSanguineo.rotulos()) {
            resposta.add(new EstoqueTipoResponse(tipo, painel.get(tipo), config.minimoPara(tipo), alertas.get(tipo)));
        }
        return resposta;
    }

    /** 200 · 404 bolsa não encontrada. */
    @GetMapping("/bolsas/{id}")
    public BolsaResponse bolsa(@PathVariable("id") String codigo) {
        return BolsaResponse.de(buscarBolsa(codigo));
    }

    /** 200 · 404 — rastreio completo da bolsa (coleta, reserva, distribuição, descarte). */
    @GetMapping("/bolsas/{id}/historico")
    public List<HistoricoResponse> historico(@PathVariable("id") String codigo) {
        buscarBolsa(codigo);
        return historicoService.historicosDaBolsa(codigo).stream().map(HistoricoResponse::de).toList();
    }

    /** 200 — bolsas a até 5 dias do vencimento (uso prioritário). */
    @GetMapping("/bolsas/alertas-validade")
    public List<BolsaResponse> alertasValidade() {
        return estoqueService.bolsasUsoPrioritario().stream().map(BolsaResponse::de).toList();
    }

    /** 200 · 400 · 404 · 409 bolsa já descartada ou distribuída. */
    @PostMapping("/bolsas/{id}/descarte")
    public BolsaResponse descartar(@PathVariable("id") String codigo, @Valid @RequestBody DescarteRequest request) {
        return BolsaResponse.de(estoqueService.descartar(codigo, request.responsavel()));
    }

    /** 200 · 400 tipo inválido — bolsas compatíveis em ordem FEFO (matriz ABO/Rh da AED). */
    @GetMapping("/compatibilidade")
    public List<BolsaResponse> compatibilidade(@RequestParam("tipo") String tipo,
                                               @RequestParam(value = "quantidade", defaultValue = "10") int quantidade) {
        if (MatrizCompatibilidade.indiceTipo(tipo) < 0) {
            throw new IllegalArgumentException("Tipo sanguíneo inválido: " + tipo);
        }
        if (quantidade < 1) {
            throw new IllegalArgumentException("quantidade deve ser pelo menos 1");
        }
        return compatibilidadeService.sugerirBolsas(tipo, quantidade).stream().map(BolsaResponse::de).toList();
    }

    private BolsaSangue buscarBolsa(String codigo) {
        BolsaSangue b = estoqueService.buscarPorCodigo(codigo);
        if (b == null) throw new RecursoNaoEncontradoException("Bolsa não encontrada");
        return b;
    }
}
