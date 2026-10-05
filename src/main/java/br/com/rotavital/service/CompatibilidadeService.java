package br.com.rotavital.service;

import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Configuracao;
import br.com.rotavital.repository.JsonDataRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CompatibilidadeService {

    private static final Map<String, List<String>> TABELA = new HashMap<>();

    static {
        TABELA.put("O-",  Arrays.asList("O-","O+","A-","A+","B-","B+","AB-","AB+"));
        TABELA.put("O+",  Arrays.asList("O+","A+","B+","AB+"));
        TABELA.put("A-",  Arrays.asList("A-","A+","AB-","AB+"));
        TABELA.put("A+",  Arrays.asList("A+","AB+"));
        TABELA.put("B-",  Arrays.asList("B-","B+","AB-","AB+"));
        TABELA.put("B+",  Arrays.asList("B+","AB+"));
        TABELA.put("AB-", Arrays.asList("AB-","AB+"));
        TABELA.put("AB+", Arrays.asList("AB+"));
    }

    private final JsonDataRepository repo;

    public CompatibilidadeService(JsonDataRepository repo) {
        this.repo = repo;
    }

    public boolean compativel(String tipoBolsa, String tipoReceptor) {
        List<String> receptores = TABELA.get(tipoBolsa);
        return receptores != null && receptores.contains(tipoReceptor);
    }

    public List<BolsaSangue> sugerirBolsas(String tipoReceptor, int quantidade) {
        List<BolsaSangue> disponiveis = repo.getEstoque().paraLista().stream()
                .filter(b -> "disponivel".equals(b.getStatus()))
                .filter(b -> compativel(b.getTipoSanguineo(), tipoReceptor))
                .toList();

        // FEFO: mesmo tipo primeiro, depois outros
        List<BolsaSangue> mesmoTipo = disponiveis.stream()
                .filter(b -> b.getTipoSanguineo().equals(tipoReceptor))
                .sorted(Comparator.comparing(BolsaSangue::getDataValidade))
                .toList();

        List<BolsaSangue> outrosTipos = disponiveis.stream()
                .filter(b -> !b.getTipoSanguineo().equals(tipoReceptor))
                .sorted(Comparator.comparing(BolsaSangue::getDataValidade))
                .toList();

        List<BolsaSangue> selecionadas = new ArrayList<>();
        selecionadas.addAll(mesmoTipo);
        selecionadas.addAll(outrosTipos);

        return selecionadas.stream().limit(quantidade).toList();
    }

    public Map<String, Object> gerarPainelEstoque() {
        List<BolsaSangue> todas = repo.getEstoque().paraLista();
        Configuracao config = repo.getConfiguracao();
        String[] tipos = {"A+","A-","B+","B-","AB+","AB-","O+","O-"};

        Map<String, Map<String, Long>> painel = new LinkedHashMap<>();
        Map<String, Boolean> alertas = new LinkedHashMap<>();

        for (String tipo : tipos) {
            Map<String, Long> counts = new LinkedHashMap<>();
            counts.put("disponivel", todas.stream().filter(b -> b.getTipoSanguineo().equals(tipo) && "disponivel".equals(b.getStatus())).count());
            counts.put("reservada", todas.stream().filter(b -> b.getTipoSanguineo().equals(tipo) && "reservada".equals(b.getStatus())).count());
            counts.put("distribuida", todas.stream().filter(b -> b.getTipoSanguineo().equals(tipo) && "distribuida".equals(b.getStatus())).count());
            counts.put("vencida", todas.stream().filter(b -> b.getTipoSanguineo().equals(tipo) && "vencida".equals(b.getStatus())).count());
            counts.put("descartada", todas.stream().filter(b -> b.getTipoSanguineo().equals(tipo) && "descartada".equals(b.getStatus())).count());
            painel.put(tipo, counts);

            int minimo = config.getMinimoEstoque().getOrDefault(tipo, 0);
            long disponivelCount = counts.get("disponivel");
            alertas.put(tipo, disponivelCount < minimo);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("painel", painel);
        resultado.put("alertas", alertas);
        return resultado;
    }
}
