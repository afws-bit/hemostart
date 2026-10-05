package br.com.rotavital.controller;

import br.com.rotavital.model.Hospital;
import br.com.rotavital.service.HospitalService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/hospitais")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("hospitais", hospitalService.listar());
        return "hospitais/lista";
    }

    @GetMapping("/novo")
    public String formNovo() {
        return "hospitais/form";
    }

    @PostMapping("/salvar")
    public String salvar(
            @RequestParam String razaoSocial,
            @RequestParam String cnpj,
            @RequestParam String endereco,
            @RequestParam String contato,
            RedirectAttributes ra) {

        Hospital h = new Hospital();
        h.setRazaoSocial(razaoSocial);
        h.setCnpj(cnpj);
        h.setEndereco(endereco);
        h.setContato(contato);

        String erro = hospitalService.cadastrar(h);
        if (erro != null) {
            ra.addFlashAttribute("erro", erro);
            return "redirect:/hospitais/novo";
        }
        ra.addFlashAttribute("sucesso", "Hospital cadastrado com sucesso!");
        return "redirect:/hospitais";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable String id, RedirectAttributes ra) {
        hospitalService.alternarStatus(id);
        ra.addFlashAttribute("sucesso", "Status do hospital atualizado.");
        return "redirect:/hospitais";
    }
}
