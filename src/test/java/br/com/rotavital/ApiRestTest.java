package br.com.rotavital;

import br.com.rotavital.model.Doador;
import br.com.rotavital.model.Hospital;
import br.com.rotavital.repository.DoadorRepository;
import br.com.rotavital.repository.HospitalRepository;
import br.com.rotavital.service.EstoqueService;
import br.com.rotavital.service.RequisicaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PI3-74 — contrato da API REST (/api/v1): DTOs na entrada/saída e erros JSON
 * padronizados pelo ApiExceptionHandler (400, 403, 404, 409).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiRestTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DoadorRepository doadorRepository;
    @Autowired private HospitalRepository hospitalRepository;
    @Autowired private EstoqueService estoqueService;
    @Autowired private RequisicaoService requisicaoService;

    private static final String DOADOR_JSON = """
            {"nome":"Ana Souza","cpf":"321.654.987-00","dataNascimento":"1990-05-10",
             "sexo":"F","tipoSanguineo":"O-","peso":65,"contato":"(81) 98888-0000",
             "historicoSaude":"Asma leve"}
            """;

    private Doador doador(String cpf, String tipo) {
        Doador d = new Doador();
        d.setNome("Doador " + cpf);
        d.setCpf(cpf);
        d.setDataNascimento(LocalDate.now().minusYears(30));
        d.setSexo("M");
        d.setTipoSanguineo(tipo);
        d.setPeso(80);
        return doadorRepository.save(d);
    }

    private Hospital hospital(String cnpj, boolean ativo) {
        Hospital h = new Hospital();
        h.setRazaoSocial("Hospital " + cnpj);
        h.setCnpj(cnpj);
        h.setAtivo(ativo);
        return hospitalRepository.save(h);
    }

    @Test
    void doadores_cadastroRetorna201SemExporDadosSensiveis() throws Exception {
        mockMvc.perform(post("/api/v1/doadores").contentType(MediaType.APPLICATION_JSON).content(DOADOR_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.cpf").value("***.654.987-**"))
                .andExpect(jsonPath("$.aptoParaDoar").value(true))
                .andExpect(jsonPath("$.historicoSaude").doesNotExist());
    }

    @Test
    void doadores_cpfDuplicadoRetorna409ComErroPadronizado() throws Exception {
        mockMvc.perform(post("/api/v1/doadores").contentType(MediaType.APPLICATION_JSON).content(DOADOR_JSON))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/doadores").contentType(MediaType.APPLICATION_JSON).content(DOADOR_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value("CPF já cadastrado"))
                .andExpect(jsonPath("$.caminho").value("/api/v1/doadores"));
    }

    @Test
    void doadores_dadosInvalidosRetornam400ComDetalhes() throws Exception {
        mockMvc.perform(post("/api/v1/doadores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"123\",\"sexo\":\"X\",\"tipoSanguineo\":\"Z+\",\"peso\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalhes", not(hasSize(0))));
    }

    @Test
    void doadores_inexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/v1/doadores/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Doador não encontrado"));
    }

    @Test
    void coletaEstoqueBolsaEHistorico() throws Exception {
        Doador d = doador("70070070070", "A+");
        String corpo = "{\"doadorId\":\"" + d.getId() + "\",\"componente\":\"Concentrado de Hemácias\",\"volume\":450}";

        mockMvc.perform(post("/api/v1/coletas").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("disponivel"))
                .andExpect(jsonPath("$.doadorId").value(d.getId()))
                .andExpect(jsonPath("$.doador").doesNotExist());

        String codigo = estoqueService.listarEstoque().get(0).getCodigo();
        mockMvc.perform(get("/api/v1/bolsas/" + codigo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoSanguineo").value("A+"));
        mockMvc.perform(get("/api/v1/bolsas/" + codigo + "/historico")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("COLETA"));
        mockMvc.perform(get("/api/v1/estoque")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));
        mockMvc.perform(get("/api/v1/bolsas/BS-99999")).andExpect(status().isNotFound());

        // mesmo doador de novo: intervalo mínimo -> 409
        mockMvc.perform(post("/api/v1/coletas").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Doador ainda não apto para nova doação"));
    }

    @Test
    void compatibilidade_tipoInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/api/v1/compatibilidade").param("tipo", "X+"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("Tipo sanguíneo inválido")));
        mockMvc.perform(get("/api/v1/compatibilidade").param("tipo", "AB+"))
                .andExpect(status().isOk());
    }

    @Test
    void solicitacoes_hospitalInativoRetorna403() throws Exception {
        hospital("44455566000177", false);
        mockMvc.perform(post("/api/v1/solicitacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpjHospital\":\"44455566000177\",\"tipoSanguineo\":\"O-\",\"quantidade\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Hospital não autorizado"));
    }

    @Test
    void solicitacoes_fluxoCompletoAteDistribuicao() throws Exception {
        hospital("55566677000188", true);
        Doador d = doador("80080080080", "O-");
        estoqueService.registrarColeta(d.getId(), "Concentrado de Hemácias", 450);

        mockMvc.perform(post("/api/v1/solicitacoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpjHospital\":\"55566677000188\",\"tipoSanguineo\":\"A+\",\"quantidade\":1,\"urgente\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pendente"));

        String id = requisicaoService.listarFila().get(0).getId();
        mockMvc.perform(post("/api/v1/solicitacoes/" + id + "/aprovacao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("aprovada"));
        mockMvc.perform(post("/api/v1/solicitacoes/" + id + "/aprovacao"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/distribuicoes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solicitacaoId\":\"" + id + "\",\"responsavel\":\"Operador\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.solicitacao.status").value("atendida"))
                .andExpect(jsonPath("$.bolsas", hasSize(1)))
                .andExpect(jsonPath("$.bolsas[0].status").value("distribuida"));

        mockMvc.perform(get("/api/v1/solicitacoes/" + id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/relatorios/distribuicoes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void corpoAusenteRetorna400() throws Exception {
        mockMvc.perform(post("/api/v1/solicitacoes").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
