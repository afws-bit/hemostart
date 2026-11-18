package br.com.rotavital.controller;

import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.TipoSanguineo;
import br.com.rotavital.service.HospitalService;
import br.com.rotavital.service.RequisicaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/requisicoes")
public class RequisicaoController {

    private final RequisicaoService requisicaoService;
    private final HospitalService hospitalService;

    public RequisicaoController(RequisicaoService requisicaoService, HospitalService hospitalService) {
        this.requisicaoService = requisicaoService;
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public String fila(Model model) {
        model.addAttribute("fila", requisicaoService.listarFila());
        model.addAttribute("hospitais", hospitalService.listar());
        return "requisicoes/fila";
    }

    @GetMapping("/nova")
    public String formNova(Model model) {
        model.addAttribute("hospitais", hospitalService.listarAtivos());
        model.addAttribute("tipos", TipoSanguineo.rotulos());
        return "requisicoes/form";
    }

    @PostMapping("/criar")
    public String criar(@RequestParam String cnpjHospital,
                        @RequestParam String tipoSanguineo,
                        @RequestParam int quantidade,
                        @RequestParam(defaultValue = "false") boolean urgente,
                        RedirectAttributes ra) {
        try {
            requisicaoService.criarRequisicao(cnpjHospital, tipoSanguineo, quantidade, urgente);
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/requisicoes/nova";
        }
        ra.addFlashAttribute("sucesso", "Solicitação registrada na fila com sucesso!");
        return "redirect:/requisicoes";
    }

    @PostMapping("/reservar")
    public String reservar(RedirectAttributes ra) {
        try {
            requisicaoService.reservarBolsasParaPrimeiro();
            ra.addFlashAttribute("sucesso", "Bolsas reservadas com sucesso! Confirme a distribuição.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/requisicoes";
    }

    /** HU13 — aprovar uma solicitação específica. */
    @PostMapping("/aprovar/{id}")
    public String aprovar(@PathVariable("id") String id, RedirectAttributes ra) {
        try {
            requisicaoService.aprovarRequisicao(id);
            ra.addFlashAttribute("sucesso", "Solicitação aprovada e bolsas reservadas com sucesso!");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/requisicoes";
    }

    /** HU13 — recusar uma solicitação com motivo. */
    @PostMapping("/recusar/{id}")
    public String recusar(@PathVariable("id") String id, @RequestParam("motivo") String motivo, RedirectAttributes ra) {
        try {
            requisicaoService.recusarRequisicao(id, motivo);
            ra.addFlashAttribute("sucesso", "Solicitação recusada com sucesso!");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/requisicoes";
    }

    @PostMapping("/distribuir/{id}")
    public String distribuir(@PathVariable String id,
                             @RequestParam String responsavel,
                             RedirectAttributes ra) {
        try {
            requisicaoService.confirmarDistribuicao(id, responsavel);
            ra.addFlashAttribute("sucesso", "Distribuição confirmada com sucesso!");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/requisicoes";
    }

    @GetMapping("/distribuir-bolsa")
    public String formDistribuirBolsa() {
        return "requisicoes/distribuir-bolsa";
    }

    @PostMapping("/distribuir-bolsa")
    public String distribuirBolsa(@RequestParam String codigoBolsa,
                                  @RequestParam String responsavel,
                                  RedirectAttributes ra) {
        try {
            requisicaoService.distribuirPorCodigo(codigoBolsa, responsavel);
            ra.addFlashAttribute("sucesso", "Bolsa distribuída com sucesso!");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/requisicoes/distribuir-bolsa";
    }
}
