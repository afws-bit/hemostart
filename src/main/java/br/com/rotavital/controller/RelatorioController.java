package br.com.rotavital.controller;

import br.com.rotavital.dto.EstatisticaDesperdicioDTO;
import br.com.rotavital.dto.EstatisticasGeraisDTO;
import br.com.rotavital.service.EstoqueService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class RelatorioController {

    @Autowired
    private EstoqueService estoqueService;

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
}