package br.com.rotavital.controller;

import br.com.rotavital.dto.PainelRedeDTO;
import br.com.rotavital.monitoramento.MonitorRede;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Painel de monitoramento de rede (RSD, U2).
 * A tela /rede consulta o JSON a cada 5 s e redesenha os cards e gráficos.
 */
@Controller
public class MonitoramentoController {

    private final MonitorRede monitor;

    public MonitoramentoController(MonitorRede monitor) {
        this.monitor = monitor;
    }

    @GetMapping("/rede")
    public String painel() {
        return "rede";
    }

    @GetMapping("/api/monitoramento/rede")
    @ResponseBody
    public PainelRedeDTO dados(@RequestParam(value = "janela", defaultValue = "300") long janelaSegundos) {
        return monitor.painel(Math.min(janelaSegundos, 3600));
    }
}
