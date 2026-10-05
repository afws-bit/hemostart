package br.com.rotavital.controller;

import br.com.rotavital.service.HistoricoService;
import br.com.rotavital.service.HospitalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/historico")
public class HistoricoController {

    private final HistoricoService historicoService;
    private final HospitalService hospitalService;

    public HistoricoController(HistoricoService historicoService,
                                  HospitalService hospitalService) {
        this.historicoService = historicoService;
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public String historico(Model model) {
        model.addAttribute("historico", historicoService.listarHistorico());
        return "historico/lista";
    }

    @GetMapping("/rastreio")
    public String rastreio(@RequestParam(required = false) String codigo, Model model) {
        if (codigo != null && !codigo.isBlank()) {
            model.addAttribute("bolsa", historicoService.buscarBolsa(codigo));
            model.addAttribute("eventos", historicoService.historicosDaBolsa(codigo));
            model.addAttribute("codigo", codigo);
        }
        return "historico/rastreio";
    }

    @GetMapping("/relatorio")
    public String relatorio(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fim,
            @RequestParam(required = false, defaultValue = "TODOS") String tipoSanguineo,
            @RequestParam(required = false, defaultValue = "TODOS") String hospitalId,
            Model model) {

        if (inicio == null) inicio = LocalDate.now().minusMonths(1);
        if (fim == null) fim = LocalDate.now();

        Map<String, Object> relatorio = historicoService.gerarRelatorio(inicio, fim, tipoSanguineo, hospitalId);
        model.addAttribute("relatorio", relatorio);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("tipoSanguineo", tipoSanguineo);
        model.addAttribute("hospitalId", hospitalId);
        model.addAttribute("tipos", new String[]{"TODOS","A+","A-","B+","B-","AB+","AB-","O+","O-"});
        model.addAttribute("hospitais", hospitalService.listar());
        return "historico/relatorio";
    }
}
