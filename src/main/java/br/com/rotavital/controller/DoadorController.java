package br.com.rotavital.controller;

import br.com.rotavital.model.Doador;
import br.com.rotavital.service.DoadorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/doadores")
public class DoadorController {

    private final DoadorService doadorService;

    public DoadorController(DoadorService doadorService) {
        this.doadorService = doadorService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("doadores", doadorService.listar());
        return "doadores/lista";
    }

    @GetMapping("/novo")
    public String formNovo(Model model) {
        model.addAttribute("doador", new Doador());
        return "doadores/form";
    }

    @PostMapping("/salvar")
    public String salvar(
            @RequestParam String nome,
            @RequestParam String cpf,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dataNascimento,
            @RequestParam String sexo,
            @RequestParam String tipoSanguineo,
            @RequestParam double peso,
            @RequestParam String contato,
            @RequestParam(defaultValue = "") String historicoSaude,
            @RequestParam(defaultValue = "false") boolean autorizacaoResponsavel,
            RedirectAttributes ra) {

        Doador d = new Doador();
        d.setNome(nome);
        d.setCpf(cpf);
        d.setDataNascimento(dataNascimento);
        d.setSexo(sexo);
        d.setTipoSanguineo(tipoSanguineo);
        d.setPeso(peso);
        d.setContato(contato);
        d.setHistoricoSaude(historicoSaude);
        d.setAutorizacaoResponsavel(autorizacaoResponsavel);

        String erro = doadorService.cadastrar(d);
        if (erro != null) {
            ra.addFlashAttribute("erro", erro);
            return "redirect:/doadores/novo";
        }
        ra.addFlashAttribute("sucesso", "Doador cadastrado com sucesso!");
        return "redirect:/doadores";
    }
}
