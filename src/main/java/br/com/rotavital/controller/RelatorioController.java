package br.com.rotavital.controller;

import br.com.rotavital.dto.EstatisticaDesperdicioDTO;
import br.com.rotavital.dto.EstatisticasGeraisDTO;
import br.com.rotavital.dto.RiscoDesabastecimentoDTO;
import br.com.rotavital.service.EstatisticaService;
import br.com.rotavital.service.EstoqueService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class RelatorioController {

    private final EstoqueService estoqueService;
    private final EstatisticaService estatisticaService;

    public RelatorioController(EstoqueService estoqueService, EstatisticaService estatisticaService) {
        this.estoqueService = estoqueService;
        this.estatisticaService = estatisticaService;
    }

    @GetMapping("/relatorios")
    public String paginaRelatorios(HttpSession session) {
        // REGRA DE SEGURANÇA: Se não houver usuário na sessão, redireciona para o login
        if (session.getAttribute("usuarioLogado") == null) {
            // Se ainda não implementaram o login, deixe comentado para testar e depois descomente:
            // return "redirect:/login"; 
        }
        return "relatorios"; // Certifique-se de que o arquivo html está no caminho correto
    }

    // Endpoint do Ticket PI3-72 (Estatísticas Gerais)
    @GetMapping("/api/estatisticas")
    @ResponseBody
    public EstatisticasGeraisDTO getEstatisticasGerais() {
        return estoqueService.calcularEstatisticasGerais();
    }

    // Endpoint do Ticket PI3-75 (Desperdício)
    @GetMapping("/api/estatisticas/desperdicio")
    @ResponseBody
    public EstatisticaDesperdicioDTO getEstatisticasDesperdicio() {
        return estoqueService.calcularEstatisticasDesperdicio();
    }

    // Endpoint do PI3-84 (EST U2: probabilidade de desabastecimento)
    @GetMapping("/api/estatisticas/desabastecimento")
    @ResponseBody
    public List<RiscoDesabastecimentoDTO> getRiscoDesabastecimento(
            @RequestParam(value = "horizonteDias", defaultValue = "7") int horizonteDias,
            @RequestParam(value = "janelaDias", defaultValue = "30") int janelaDias) {
        return estatisticaService.riscoDesabastecimento(horizonteDias, janelaDias);
    }
}
