package br.com.rotavital.repository;

import br.com.rotavital.estruturas.FilaRequisicoes;
import br.com.rotavital.estruturas.ListaEncadeada;
import br.com.rotavital.estruturas.PilhaHistorico;
import br.com.rotavital.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Repository
public class JsonDataRepository {

    @Value("${data.dir:data}")
    private String dataDir;

    private final ObjectMapper mapper;
    private final ReentrantLock lock = new ReentrantLock();

    private List<Doador> doadores = new ArrayList<>();
    private List<Hospital> hospitais = new ArrayList<>();
    private ListaEncadeada<BolsaSangue> estoque = new ListaEncadeada<>();
    private FilaRequisicoes filaRequisicoes = new FilaRequisicoes();
    private PilhaHistorico<HistoricoOperacao> historicoStack = new PilhaHistorico<>();
    private Configuracao configuracao = new Configuracao();

    private AtomicInteger sequencialBolsa = new AtomicInteger(0);

    public JsonDataRepository() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() {
        File dir = new File(dataDir);
        if (!dir.exists()) dir.mkdirs();
        carregar();
    }

    private void carregar() {
        doadores = lerJson(arquivo("donors.json"), new TypeReference<List<Doador>>() {});
        hospitais = lerJson(arquivo("hospitals.json"), new TypeReference<List<Hospital>>() {});
        List<BolsaSangue> bolsas = lerJson(arquivo("bags.json"), new TypeReference<List<BolsaSangue>>() {});
        List<Requisicao> reqs = lerJson(arquivo("requests.json"), new TypeReference<List<Requisicao>>() {});
        List<HistoricoOperacao> hist = lerJson(arquivo("history.json"), new TypeReference<List<HistoricoOperacao>>() {});
        Configuracao cfg = lerJsonSingle(arquivo("config.json"), Configuracao.class);

        if (doadores == null) doadores = new ArrayList<>();
        if (hospitais == null) hospitais = new ArrayList<>();
        if (bolsas == null) bolsas = new ArrayList<>();
        if (reqs == null) reqs = new ArrayList<>();
        if (hist == null) hist = new ArrayList<>();
        if (cfg != null) configuracao = cfg;

        estoque = new ListaEncadeada<>();
        Comparator<BolsaSangue> porValidade = Comparator.comparing(BolsaSangue::getDataValidade);
        for (BolsaSangue b : bolsas) {
            estoque.inserirOrdenado(b, porValidade);
        }

        filaRequisicoes = new FilaRequisicoes();
        for (Requisicao r : reqs) {
            if ("pendente".equals(r.getStatus()) || "aprovada".equals(r.getStatus())) {
                filaRequisicoes.enfileirar(r);
            }
        }

        historicoStack = new PilhaHistorico<>();
        List<HistoricoOperacao> histReverso = new ArrayList<>(hist);
        for (HistoricoOperacao h : histReverso) {
            historicoStack.empilhar(h);
        }

        // calcular sequencial
        int max = 0;
        for (BolsaSangue b : bolsas) {
            if (b.getCodigo() != null && b.getCodigo().startsWith("BS-")) {
                try {
                    int num = Integer.parseInt(b.getCodigo().substring(3));
                    if (num > max) max = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        sequencialBolsa.set(max);

        if (doadores.isEmpty() && hospitais.isEmpty() && bolsas.isEmpty()) {
            gerarDadosIniciais();
        }
    }

    private void gerarDadosIniciais() {
        // Doadores
        Doador d1 = new Doador();
        d1.setId(UUID.randomUUID().toString());
        d1.setNome("Carlos Alberto Silva");
        d1.setCpf("12345678901");
        d1.setDataNascimento(LocalDate.of(1985, 3, 15));
        d1.setSexo("M");
        d1.setTipoSanguineo("O+");
        d1.setPeso(78.0);
        d1.setContato("(11) 98765-4321");
        d1.setHistoricoSaude("Sem comorbidades.");
        doadores.add(d1);

        Doador d2 = new Doador();
        d2.setId(UUID.randomUUID().toString());
        d2.setNome("Maria Fernanda Costa");
        d2.setCpf("98765432100");
        d2.setDataNascimento(LocalDate.of(1992, 7, 22));
        d2.setSexo("F");
        d2.setTipoSanguineo("A-");
        d2.setPeso(62.0);
        d2.setContato("(11) 91234-5678");
        d2.setHistoricoSaude("Hipertensão controlada.");
        doadores.add(d2);

        Doador d3 = new Doador();
        d3.setId(UUID.randomUUID().toString());
        d3.setNome("João Paulo Mendes");
        d3.setCpf("11122233344");
        d3.setDataNascimento(LocalDate.of(1978, 11, 5));
        d3.setSexo("M");
        d3.setTipoSanguineo("B+");
        d3.setPeso(85.0);
        d3.setContato("(21) 99876-5432");
        d3.setHistoricoSaude("Sem restrições.");
        d3.setUltimaDoacao(LocalDate.now().minusDays(100));
        doadores.add(d3);

        // Hospitais
        Hospital h1 = new Hospital();
        h1.setId(UUID.randomUUID().toString());
        h1.setRazaoSocial("Hospital Santa Cruz");
        h1.setCnpj("12345678000195");
        h1.setEndereco("Rua das Flores, 100, São Paulo/SP");
        h1.setContato("(11) 3333-4444");
        h1.setAtivo(true);
        hospitais.add(h1);

        Hospital h2 = new Hospital();
        h2.setId(UUID.randomUUID().toString());
        h2.setRazaoSocial("Hospital Municipal Norte");
        h2.setCnpj("98765432000188");
        h2.setEndereco("Av. Brasil, 500, Rio de Janeiro/RJ");
        h2.setContato("(21) 2222-3333");
        h2.setAtivo(true);
        hospitais.add(h2);

        Hospital h3 = new Hospital();
        h3.setId(UUID.randomUUID().toString());
        h3.setRazaoSocial("Clínica São Lucas (Inativa)");
        h3.setCnpj("11122233000177");
        h3.setEndereco("Rua do Comércio, 200, Belo Horizonte/MG");
        h3.setContato("(31) 4444-5555");
        h3.setAtivo(false);
        hospitais.add(h3);

        // Bolsas
        LocalDate hoje = LocalDate.now();
        Comparator<BolsaSangue> porValidade = Comparator.comparing(BolsaSangue::getDataValidade);

        criarBolsa(d1, "Concentrado de Hemácias", 450, hoje.minusDays(5), porValidade);
        criarBolsa(d1, "Concentrado de Hemácias", 450, hoje.minusDays(10), porValidade);
        criarBolsa(d2, "Plaquetas", 200, hoje.minusDays(3), porValidade);
        criarBolsa(d3, "Plasma", 300, hoje.minusDays(30), porValidade);
        criarBolsa(d3, "Concentrado de Hemácias", 450, hoje.minusDays(20), porValidade);

        // bolsa vencendo em 4 dias
        BolsaSangue bPrioritaria = criarBolsaRetorno(d1, "Concentrado de Hemácias", 450, hoje.minusDays(38), porValidade);

        // bolsa já vencida
        BolsaSangue bVencida = criarBolsaRetorno(d2, "Plaquetas", 200, hoje.minusDays(10), porValidade);
        bVencida.setStatus("vencida");

        gravar();
    }

    private BolsaSangue criarBolsaRetorno(Doador doador, String componente, int volume, LocalDate dataColeta, Comparator<BolsaSangue> comp) {
        BolsaSangue bolsa = new BolsaSangue();
        bolsa.setCodigo(gerarCodigoBolsa());
        bolsa.setTipoSanguineo(doador.getTipoSanguineo());
        bolsa.setComponente(componente);
        bolsa.setVolume(volume);
        bolsa.setDoadorId(doador.getId());
        bolsa.setDataColeta(dataColeta);
        bolsa.setDataValidade(dataColeta.plusDays(validadeDias(componente)));
        bolsa.setStatus("disponivel");
        estoque.inserirOrdenado(bolsa, comp);
        return bolsa;
    }

    private void criarBolsa(Doador doador, String componente, int volume, LocalDate dataColeta, Comparator<BolsaSangue> comp) {
        criarBolsaRetorno(doador, componente, volume, dataColeta, comp);
    }

    public int validadeDias(String componente) {
        return switch (componente) {
            case "Concentrado de Hemácias" -> 42;
            case "Plaquetas" -> 5;
            case "Plasma" -> 365;
            default -> 42;
        };
    }

    public String gerarCodigoBolsa() {
        int num = sequencialBolsa.incrementAndGet();
        return String.format("BS-%05d", num);
    }

    public void gravar() {
        lock.lock();
        try {
            gravarJson(arquivo("donors.json"), doadores);
            gravarJson(arquivo("hospitals.json"), hospitais);
            gravarJson(arquivo("bags.json"), estoque.paraLista());
            List<Requisicao> todasReqs = filaRequisicoes.paraLista();
            gravarJson(arquivo("requests.json"), todasReqs);
            List<HistoricoOperacao> histLista = historicoStack.paraLista();
            java.util.Collections.reverse(histLista);
            gravarJson(arquivo("history.json"), histLista);
            gravarJsonSingle(arquivo("config.json"), configuracao);
        } finally {
            lock.unlock();
        }
    }

    private File arquivo(String nome) {
        return new File(dataDir + File.separator + nome);
    }

    private <T> T lerJson(File f, TypeReference<T> type) {
        if (!f.exists()) return null;
        try {
            return mapper.readValue(f, type);
        } catch (IOException e) {
            return null;
        }
    }

    private <T> T lerJsonSingle(File f, Class<T> clazz) {
        if (!f.exists()) return null;
        try {
            return mapper.readValue(f, clazz);
        } catch (IOException e) {
            return null;
        }
    }

    private void gravarJson(File f, Object obj) {
        try {
            mapper.writeValue(f, obj);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao gravar " + f.getName(), e);
        }
    }

    private void gravarJsonSingle(File f, Object obj) {
        gravarJson(f, obj);
    }

    // Getters/Setters para as estruturas
    public List<Doador> getDoadores() { return doadores; }
    public List<Hospital> getHospitais() { return hospitais; }
    public ListaEncadeada<BolsaSangue> getEstoque() { return estoque; }
    public FilaRequisicoes getFilaRequisicoes() { return filaRequisicoes; }
    public PilhaHistorico<HistoricoOperacao> getHistoricoStack() { return historicoStack; }
    public Configuracao getConfiguracao() { return configuracao; }
    public ReentrantLock getLock() { return lock; }
}
