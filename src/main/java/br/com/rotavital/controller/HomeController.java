package br.com.rotavital.controller;

import br.com.rotavital.service.CompatibilidadeService;
import br.com.rotavital.service.EstoqueService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class HomeController {

    private final CompatibilidadeService compatibilidadeService;
    private final EstoqueService estoqueService;

    public HomeController(CompatibilidadeService compatibilidadeService,
                           EstoqueService estoqueService) {
        this.compatibilidadeService = compatibilidadeService;
        this.estoqueService = estoqueService;
    }

    @GetMapping("/")
    public String home(Model model) {
        Map<String, Object> dados = compatibilidadeService.gerarPainelEstoque();
        model.addAttribute("alertas", dados.get("alertas"));
        model.addAttribute("usoPrioritario", estoqueService.bolsasUsoPrioritario().size());
        model.addAttribute("vencidas", estoqueService.bolsasVencidas().size());
        return "home";
    }
}
