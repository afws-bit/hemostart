package br.com.rotavital;

import br.com.rotavital.monitoramento.SondaBanco;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RSD (U2) — painel de monitoramento de rede ponta a ponta: o filtro mede as requisições,
 * a sonda mede o banco e o endpoint devolve as métricas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PainelRedeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private SondaBanco sondaBanco;

    @Test
    void filtroRegistraRequisicoesEPainelMostraAsLigacoes() throws Exception {
        mockMvc.perform(get("/api/v1/estoque")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/bolsas/BS-99999")).andExpect(status().isNotFound());
        sondaBanco.medir();

        mockMvc.perform(get("/api/monitoramento/rede").param("janela", "300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ligacoes", hasSize(2)))
                .andExpect(jsonPath("$.ligacoes[0].nome").value("Cliente → Aplicação"))
                .andExpect(jsonPath("$.ligacoes[0].amostras", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.ligacoes[1].nome").value("Aplicação → Banco"))
                .andExpect(jsonPath("$.ligacoes[1].amostras", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.ligacoes[1].perdaPercentual").value(0.0))
                .andExpect(jsonPath("$.rotas[*].rota", hasItem("GET /api/v1/estoque")))
                .andExpect(jsonPath("$.rotas[*].rota", hasItem("GET /api/v1/bolsas/{id}")));
    }

    @Test
    void consultaAoPainelNaoEntraNasMetricas() throws Exception {
        mockMvc.perform(get("/api/monitoramento/rede")).andExpect(status().isOk());
        mockMvc.perform(get("/api/monitoramento/rede"))
                .andExpect(jsonPath("$.rotas[*].rota", org.hamcrest.Matchers.not(hasItem("GET /api/monitoramento/rede"))));
    }

    @Test
    void telaDoPainelAbre() throws Exception {
        mockMvc.perform(get("/rede"))
                .andExpect(status().isOk())
                .andExpect(view().name("rede"));
    }
}
