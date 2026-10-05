package br.com.rotavital.controller;

import br.com.rotavital.service.BenchmarkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;

    public BenchmarkController(BenchmarkService benchmarkService) {
        this.benchmarkService = benchmarkService;
    }

    @GetMapping
    public String pagina() {
        return "benchmark/pagina";
    }

    @PostMapping("/executar")
    public String executar(Model model) {
        try {
            model.addAttribute("resultado", benchmarkService.executarBenchmark());
        } catch (Exception e) {
            model.addAttribute("erro", "Erro ao executar benchmark: " + e.getMessage());
        }
        return "benchmark/pagina";
    }
}
