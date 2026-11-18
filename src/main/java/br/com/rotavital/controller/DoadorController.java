package br.com.rotavital.controller;

import br.com.rotavital.exception.RegraNegocioException;
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
    public String salvar(@RequestParam String nome,
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

        try {
            doadorService.cadastrar(d);
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/doadores/novo";
        }
        ra.addFlashAttribute("sucesso", "Doador cadastrado com sucesso!");
        return "redirect:/doadores";
    }

    /** HU14 — histórico de doações e próxima data apta. */
    @GetMapping("/{id}/historico")
    public String historico(@PathVariable("id") String id, Model model, RedirectAttributes ra) {
        try {
            Doador doador = doadorService.buscarPorId(id);
            model.addAttribute("doador", doador);
            model.addAttribute("doacoes", doadorService.listarDoacoesDoador(id));
            model.addAttribute("proximaData", doadorService.calcularProximaDataApta(doador));
            model.addAttribute("apto", doadorService.estaApto(doador));
            return "doadores/historico";
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/doadores";
        }
    }
}
