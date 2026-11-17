package br.com.rotavital.controller;

import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Componente;
import br.com.rotavital.service.DoadorService;
import br.com.rotavital.service.EstoqueService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/coleta")
public class ColetaController {

    private final EstoqueService estoqueService;
    private final DoadorService doadorService;

    public ColetaController(EstoqueService estoqueService, DoadorService doadorService) {
        this.estoqueService = estoqueService;
        this.doadorService = doadorService;
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("doadores", doadorService.listar());
        model.addAttribute("componentes", Componente.descricoes());
        return "coleta/form";
    }

    @PostMapping("/registrar")
    public String registrar(@RequestParam String doadorId,
                            @RequestParam String componente,
                            @RequestParam int volume,
                            RedirectAttributes ra) {
        try {
            BolsaSangue bolsa = estoqueService.registrarColeta(doadorId, componente, volume);
            ra.addFlashAttribute("sucesso", "Coleta registrada com sucesso! Bolsa " + bolsa.getCodigo()
                    + " válida até " + bolsa.getDataValidade());
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/coleta";
    }
}
