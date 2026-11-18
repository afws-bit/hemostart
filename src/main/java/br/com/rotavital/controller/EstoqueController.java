package br.com.rotavital.controller;

import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.TipoSanguineo;
import br.com.rotavital.service.CompatibilidadeService;
import br.com.rotavital.service.ConfiguracaoService;
import br.com.rotavital.service.EstoqueService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/estoque")
public class EstoqueController {

    private final CompatibilidadeService compatibilidadeService;
    private final EstoqueService estoqueService;
    private final ConfiguracaoService configuracaoService;

    public EstoqueController(CompatibilidadeService compatibilidadeService,
                             EstoqueService estoqueService,
                             ConfiguracaoService configuracaoService) {
        this.compatibilidadeService = compatibilidadeService;
        this.estoqueService = estoqueService;
        this.configuracaoService = configuracaoService;
    }

    @GetMapping
    public String painel(Model model) {
        Map<String, Object> dados = compatibilidadeService.gerarPainelEstoque();
        model.addAttribute("painel", dados.get("painel"));
        model.addAttribute("alertas", dados.get("alertas"));
        model.addAttribute("configuracao", configuracaoService.obter());
        model.addAttribute("usoPrioritario", estoqueService.bolsasUsoPrioritario());
        model.addAttribute("bolsas", estoqueService.listarEstoque());
        return "estoque/painel";
    }

    @GetMapping("/verificar")
    public String verificar(RedirectAttributes ra) {
        estoqueService.verificarValidade();
        ra.addFlashAttribute("sucesso", "Verificação de validade executada com sucesso!");
        return "redirect:/estoque";
    }

    @GetMapping("/validade")
    public String validade(Model model) {
        model.addAttribute("vencidas", estoqueService.bolsasVencidas());
        model.addAttribute("usoPrioritario", estoqueService.bolsasUsoPrioritario());
        return "estoque/validade";
    }

    @PostMapping("/descartar")
    public String descartar(@RequestParam String codigo,
                            @RequestParam String responsavel,
                            RedirectAttributes ra) {
        try {
            estoqueService.descartar(codigo, responsavel);
            ra.addFlashAttribute("sucesso", "Bolsa descartada com sucesso!");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/estoque/validade";
    }

    @GetMapping("/configurar")
    public String configurar(Model model) {
        model.addAttribute("configuracao", configuracaoService.obter());
        model.addAttribute("tipos", TipoSanguineo.rotulos());
        return "estoque/configurar";
    }

    @PostMapping("/configurar/salvar")
    public String salvarConfig(@RequestParam Map<String, String> params, RedirectAttributes ra) {
        configuracaoService.salvarNiveis(params);
        ra.addFlashAttribute("sucesso", "Configuração salva com sucesso!");
        return "redirect:/estoque/configurar";
    }
}
