package br.com.rotavital.controller;

import br.com.rotavital.service.EstoqueService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/coleta")
public class ColetaController {

    private final EstoqueService estoqueService;
    private final br.com.rotavital.service.DoadorService doadorService;

    public ColetaController(EstoqueService estoqueService,
                              br.com.rotavital.service.DoadorService doadorService) {
        this.estoqueService = estoqueService;
        this.doadorService = doadorService;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("doadores", doadorService.listar());
        model.addAttribute("componentes", new String[]{
                "Concentrado de Hemácias", "Plaquetas", "Plasma"
        });
        return "coleta/form";
    }

    @PostMapping("/registrar")
    public String registrar(
            @RequestParam String doadorId,
            @RequestParam String componente,
            @RequestParam int volume,
            RedirectAttributes ra) {

        String erro = estoqueService.registrarColeta(doadorId, componente, volume);
        if (erro != null) {
            ra.addFlashAttribute("erro", erro);
            return "redirect:/coleta";
        }
        ra.addFlashAttribute("sucesso", "Coleta registrada com sucesso!");
        return "redirect:/coleta";
    }
}
