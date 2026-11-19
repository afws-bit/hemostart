package br.com.rotavital;

import br.com.rotavital.model.Doador;
import br.com.rotavital.repository.DoadorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RSD (U2) — projeto de observabilidade: Actuator expõe as métricas no formato Prometheus com a tag
 * application, as métricas de negócio são registradas e os spans de serviço e de repositório existem.
 * (Spans viram timers hemostart_servico_seconds e hemostart_repositorio_seconds, então dá para
 * verificar a instrumentação sem o Tempo.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability(tracing = false)
@Transactional
class ObservabilidadeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DoadorRepository doadorRepository;

    @Test
    void actuatorExpoeHealthEInfo() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics/http.server.requests")).andExpect(status().isOk());
    }

    @Test
    void prometheusTemMetricasPadraoComATagApplication() throws Exception {
        mockMvc.perform(get("/api/v1/estoque")).andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("application=\"hemostart\"")))
                .andExpect(content().string(containsString("http_server_requests_seconds")))
                .andExpect(content().string(containsString("jvm_memory_used_bytes")))
                .andExpect(content().string(containsString("process_uptime_seconds")))
                .andExpect(content().string(containsString("hikaricp_connections")))
                .andExpect(content().string(containsString("hemostart_estoque_disponivel")))
                .andExpect(content().string(containsString("hemostart_solicitacoes_pendentes")));
    }

    @Test
    void coletaGeraMetricaDeNegocioESpansDeServicoEBanco() throws Exception {
        Doador d = new Doador();
        d.setNome("Doador Observabilidade");
        d.setCpf("55566677788");
        d.setDataNascimento(LocalDate.now().minusYears(30));
        d.setSexo("M");
        d.setTipoSanguineo("AB-");
        d.setPeso(80);
        d = doadorRepository.save(d);

        String corpo = "{\"doadorId\":\"" + d.getId() + "\",\"componente\":\"Plasma\",\"volume\":450}";
        mockMvc.perform(post("/api/v1/coletas").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hemostart_bolsas_coletadas_total{")))
                .andExpect(content().string(containsString("tipo=\"AB-\"")))
                .andExpect(content().string(containsString("hemostart_servico_seconds_count{")))
                .andExpect(content().string(containsString("metodo=\"registrarColeta\"")))
                .andExpect(content().string(containsString("classe=\"SecaoCritica\"")))
                .andExpect(content().string(containsString("hemostart_repositorio_seconds_count{")))
                .andExpect(content().string(containsString("classe=\"DoadorRepository\"")));
    }

    @Test
    void endpointDeErroGera500ParaADemonstracao() throws Exception {
        mockMvc.perform(get("/api/teste/erro"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }
}
