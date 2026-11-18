package br.com.rotavital.controller.api;

import br.com.rotavital.dto.api.RelatorioResponse;
import br.com.rotavital.service.HistoricoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/** Contrato de API — relatórios (HU08). Filtros por query string. */
@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatorioApiController {

    private final HistoricoService historicoService;

    public RelatorioApiController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping("/doacoes")
    public RelatorioResponse doacoes(
            @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(value = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(value = "tipoSanguineo", defaultValue = "TODOS") String tipo,
            @RequestParam(value = "hospitalId", defaultValue = "TODOS") String hospitalId) {
        return gerar("doacoes", inicio, fim, tipo, hospitalId);
    }

    @GetMapping("/distribuicoes")
    public RelatorioResponse distribuicoes(
            @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(value = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(value = "tipoSanguineo", defaultValue = "TODOS") String tipo,
            @RequestParam(value = "hospitalId", defaultValue = "TODOS") String hospitalId) {
        return gerar("distribuicoes", inicio, fim, tipo, hospitalId);
    }

    private RelatorioResponse gerar(String nome, LocalDate inicio, LocalDate fim, String tipo, String hospitalId) {
        LocalDate i = inicio != null ? inicio : LocalDate.now().minusMonths(1);
        LocalDate f = fim != null ? fim : LocalDate.now();
        if (f.isBefore(i)) throw new IllegalArgumentException("fim deve ser igual ou posterior a inicio");
        Map<String, Object> r = historicoService.gerarRelatorio(i, f, tipo, hospitalId);
        long coletadas = (Long) r.get("coletadas");
        long distribuidas = (Long) r.get("distribuidas");
        long descartadas = (Long) r.get("descartadas");
        long total = "doacoes".equals(nome) ? coletadas : distribuidas;
        return new RelatorioResponse(nome, i, f, tipo, hospitalId, total, coletadas, distribuidas, descartadas);
    }
}
