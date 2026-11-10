package br.com.rotavital.controller;

import br.com.rotavital.dto.EstatisticaDesperdicioDTO;
import br.com.rotavital.service.EstoqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class RelatorioController {

    @Autowired
    private EstoqueService estoqueService;

    // Rota que abre a página HTML
    @GetMapping("/relatorios")
    public String paginaRelatorios() {
        return "relatorios"; // Vai buscar o arquivo relatorios.html
    }

    // Rota da API que retorna os dados em JSON para o gráfico
    @GetMapping("/api/estatisticas/desperdicio")
    @ResponseBody
    public EstatisticaDesperdicioDTO getEstatisticasDesperdicio() {
        return estoqueService.calcularEstatisticasDesperdicio();
    }
}