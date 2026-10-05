package br.com.rotavital.controller;

import br.com.rotavital.service.CompatibilidadeService;
import br.com.rotavital.service.EstoqueService;
import br.com.rotavital.repository.JsonDataRepository;
import br.com.rotavital.model.Configuracao;
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
    private final JsonDataRepository repo;

    public EstoqueController(CompatibilidadeService compatibilidadeService,
                               EstoqueService estoqueService,
                               JsonDataRepository repo) {
        this.compatibilidadeService = compatibilidadeService;
        this.estoqueService = estoqueService;
        this.repo = repo;
    }

    @GetMapping
    public String painel(Model model) {
        Map<String, Object> dados = compatibilidadeService.gerarPainelEstoque();
        model.addAttribute("painel", dados.get("painel"));
        model.addAttribute("alertas", dados.get("alertas"));
        model.addAttribute("configuracao", repo.getConfiguracao());
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
        String erro = estoqueService.descartar(codigo, responsavel);
        if (erro != null) {
            ra.addFlashAttribute("erro", erro);
        } else {
            ra.addFlashAttribute("sucesso", "Bolsa descartada com sucesso!");
        }
        return "redirect:/estoque/validade";
    }

    @GetMapping("/configurar")
    public String configurar(Model model) {
        model.addAttribute("configuracao", repo.getConfiguracao());
        model.addAttribute("tipos", new String[]{"A+","A-","B+","B-","AB+","AB-","O+","O-"});
        return "estoque/configurar";
    }

    @PostMapping("/configurar/salvar")
    public String salvarConfig(
            @RequestParam Map<String, String> params,
            RedirectAttributes ra) {
        Configuracao config = repo.getConfiguracao();
        String[] tipos = {"A+","A-","B+","B-","AB+","AB-","O+","O-"};
        for (String tipo : tipos) {
            String key = "minimo_" + tipo.replace("+", "pos").replace("-", "neg");
            if (params.containsKey(key)) {
                try {
                    config.getMinimoEstoque().put(tipo, Integer.parseInt(params.get(key)));
                } catch (NumberFormatException ignored) {}
            }
        }
        repo.gravar();
        ra.addFlashAttribute("sucesso", "Configuração salva com sucesso!");
        return "redirect:/estoque/configurar";
    }
}
