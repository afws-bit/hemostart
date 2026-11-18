package br.com.rotavital.controller;

import br.com.rotavital.model.Doador;
import br.com.rotavital.model.TriagemClinica;
import br.com.rotavital.service.DoadorService;
import br.com.rotavital.service.TriagemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/triagem")
public class TriagemController {

    private final TriagemService triagemService;
    private final DoadorService doadorService;

    public TriagemController(TriagemService triagemService, DoadorService doadorService) {
        this.triagemService = triagemService;
        this.doadorService = doadorService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("triagens", triagemService.listarTodas());
        model.addAttribute("doadores", doadorService.listar());
        return "triagem/lista";
    }

    @GetMapping("/nova")
    public String formNova(@RequestParam(value = "doadorId", required = false) String doadorId, Model model) {
        model.addAttribute("doadores", doadorService.listar());
        model.addAttribute("doadorIdSelecionado", doadorId);
        return "triagem/form";
    }

    @PostMapping("/salvar")
    public String salvar(
            @RequestParam("doadorId") String doadorId,
            @RequestParam("peso") double peso,
            @RequestParam("pressaoArterial") String pressaoArterial,
            @RequestParam("temperatura") double temperatura,
            @RequestParam("hemoglobina") double hemoglobina,
            RedirectAttributes ra) {

        try {
            TriagemClinica triagem = triagemService.registrarTriagem(doadorId, peso, pressaoArterial, temperatura, hemoglobina);
            if ("apto".equalsIgnoreCase(triagem.getResultado())) {
                ra.addFlashAttribute("sucesso", "Triagem registrada com sucesso: Doador apto para doação!");
            } else {
                ra.addFlashAttribute("erro", "Doador classificado como inapto na triagem. Motivo: " + triagem.getMotivoInaptidao());
            }
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/triagem/nova" + (doadorId != null ? "?doadorId=" + doadorId : "");
        }
        return "redirect:/triagem";
    }
}
