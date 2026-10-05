package br.com.rotavital.controller;

import br.com.rotavital.model.Hospital;
import br.com.rotavital.model.Requisicao;
import br.com.rotavital.service.HospitalService;
import br.com.rotavital.service.RequisicaoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/requisicoes")
public class RequisicaoController {

    private final RequisicaoService requisicaoService;
    private final HospitalService hospitalService;

    public RequisicaoController(RequisicaoService requisicaoService,
                                  HospitalService hospitalService) {
        this.requisicaoService = requisicaoService;
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public String fila(Model model) {
        List<Requisicao> fila = requisicaoService.listarFila();
        List<Hospital> hospitais = hospitalService.listar();
        model.addAttribute("fila", fila);
        model.addAttribute("hospitais", hospitais);
        return "requisicoes/fila";
    }

    @GetMapping("/nova")
    public String formNova(Model model) {
        model.addAttribute("hospitais", hospitalService.listar().stream()
                .filter(Hospital::isAtivo).toList());
        model.addAttribute("tipos", new String[]{"A+","A-","B+","B-","AB+","AB-","O+","O-"});
        return "requisicoes/form";
    }

    @PostMapping("/criar")
    public String criar(
            @RequestParam String cnpjHospital,
            @RequestParam String tipoSanguineo,
            @RequestParam int quantidade,
            @RequestParam(defaultValue = "false") boolean urgente,
            RedirectAttributes ra) {

        String erro = requisicaoService.criarRequisicao(cnpjHospital, tipoSanguineo, quantidade, urgente);
        if (erro != null) {
            ra.addFlashAttribute("erro", erro);
            return "redirect:/requisicoes/nova";
        }
        ra.addFlashAttribute("sucesso", "Solicitação registrada na fila com sucesso!");
        return "redirect:/requisicoes";
    }

    @PostMapping("/reservar")
    public String reservar(RedirectAttributes ra) {
        String erro = requisicaoService.reservarBolsasParaPrimeiro();
        if (erro != null) {
            ra.addFlashAttribute("erro", erro);
        } else {
            ra.addFlashAttribute("sucesso", "Bolsas reservadas com sucesso! Confirme a distribuição.");
        }
        return "redirect:/requisicoes";
    }

    @PostMapping("/distribuir/{id}")
    public String distribuir(
            @PathVariable String id,
            @RequestParam String responsavel,
            RedirectAttributes ra) {

        String erro = requisicaoService.confirmarDistribuicao(id, responsavel);
        if (erro != null) {
            ra.addFlashAttribute("erro", erro);
        } else {
            ra.addFlashAttribute("sucesso", "Distribuição confirmada com sucesso!");
        }
        return "redirect:/requisicoes";
    }

    @GetMapping("/distribuir-bolsa")
    public String formDistribuirBolsa() {
        return "requisicoes/distribuir-bolsa";
    }

    @PostMapping("/distribuir-bolsa")
    public String distribuirBolsa(
            @RequestParam String codigoBolsa,
            @RequestParam String responsavel,
            RedirectAttributes ra) {

        String erro = requisicaoService.distribuirPorCodigo(codigoBolsa, responsavel);
        if (erro != null) {
            ra.addFlashAttribute("erro", erro);
        } else {
            ra.addFlashAttribute("sucesso", "Bolsa distribuída com sucesso!");
        }
        return "redirect:/requisicoes/distribuir-bolsa";
    }
}
