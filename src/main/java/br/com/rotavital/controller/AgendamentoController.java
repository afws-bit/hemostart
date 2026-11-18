package br.com.rotavital.controller;

import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.service.AgendamentoService;
import br.com.rotavital.service.DoadorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final DoadorService doadorService;

    public AgendamentoController(AgendamentoService agendamentoService, DoadorService doadorService) {
        this.agendamentoService = agendamentoService;
        this.doadorService = doadorService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("agendamentos", agendamentoService.listar());
        model.addAttribute("doadores", doadorService.listar());
        return "agendamentos/lista";
    }

    @GetMapping("/novo")
    public String formNovo(@RequestParam(value = "doadorId", required = false) String doadorId, Model model) {
        model.addAttribute("doadores", doadorService.listar());
        model.addAttribute("doadorIdSelecionado", doadorId);
        model.addAttribute("horarios", new String[]{
                "08:00", "08:30", "09:00", "09:30", "10:00", "10:30",
                "11:00", "14:00", "14:30", "15:00", "15:30", "16:00"
        });
        model.addAttribute("dataMinima", LocalDate.now());
        return "agendamentos/form";
    }

    @PostMapping("/salvar")
    public String salvar(
            @RequestParam("doadorId") String doadorId,
            @RequestParam("data") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate data,
            @RequestParam("horario") String horario,
            RedirectAttributes ra) {

        try {
            agendamentoService.agendar(doadorId, data, horario);
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/agendamentos/novo" + (doadorId != null ? "?doadorId=" + doadorId : "");
        }
        ra.addFlashAttribute("sucesso", "Agendamento realizado com sucesso!");
        return "redirect:/agendamentos";
    }

    @PostMapping("/cancelar/{id}")
    public String cancelar(@PathVariable("id") String id, RedirectAttributes ra) {
        try {
            agendamentoService.cancelar(id);
            ra.addFlashAttribute("sucesso", "Agendamento cancelado com sucesso!");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/agendamentos";
    }
}
