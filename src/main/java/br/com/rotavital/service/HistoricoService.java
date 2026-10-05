package br.com.rotavital.service;

import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.HistoricoOperacao;
import br.com.rotavital.repository.JsonDataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class HistoricoService {

    private final JsonDataRepository repo;

    public HistoricoService(JsonDataRepository repo) {
        this.repo = repo;
    }

    public List<HistoricoOperacao> listarHistorico() {
        return repo.getHistoricoStack().paraLista();
    }

    public List<BolsaSangue> rastrearBolsa(String codigo) {
        return repo.getEstoque().paraLista().stream()
                .filter(b -> b.getCodigo().equals(codigo))
                .toList();
    }

    public BolsaSangue buscarBolsa(String codigo) {
        return repo.getEstoque().buscar(b -> b.getCodigo().equals(codigo));
    }

    public List<HistoricoOperacao> historicosDaBolsa(String codigo) {
        return repo.getHistoricoStack().paraLista().stream()
                .filter(h -> codigo.equals(h.getCodigoBolsa()))
                .toList();
    }

    public Map<String, Object> gerarRelatorio(LocalDate inicio, LocalDate fim,
                                               String tipoSanguineo, String hospitalId) {
        List<BolsaSangue> todas = repo.getEstoque().paraLista();

        List<BolsaSangue> filtradas = todas.stream()
                .filter(b -> {
                    if (tipoSanguineo != null && !tipoSanguineo.isBlank() && !tipoSanguineo.equals("TODOS")) {
                        if (!b.getTipoSanguineo().equals(tipoSanguineo)) return false;
                    }
                    if (hospitalId != null && !hospitalId.isBlank() && !hospitalId.equals("TODOS")) {
                        if (!hospitalId.equals(b.getHospitalDestinoId())) return false;
                    }
                    return true;
                })
                .toList();

        long coletadas = filtradas.stream()
                .filter(b -> b.getDataColeta() != null
                        && !b.getDataColeta().isBefore(inicio)
                        && !b.getDataColeta().isAfter(fim))
                .count();

        long distribuidas = filtradas.stream()
                .filter(b -> b.getDataDistribuicao() != null
                        && !b.getDataDistribuicao().toLocalDate().isBefore(inicio)
                        && !b.getDataDistribuicao().toLocalDate().isAfter(fim))
                .count();

        long descartadas = filtradas.stream()
                .filter(b -> b.getDataDescarte() != null
                        && !b.getDataDescarte().toLocalDate().isBefore(inicio)
                        && !b.getDataDescarte().toLocalDate().isAfter(fim))
                .count();

        java.util.Map<String, Object> relatorio = new java.util.LinkedHashMap<>();
        relatorio.put("coletadas", coletadas);
        relatorio.put("distribuidas", distribuidas);
        relatorio.put("descartadas", descartadas);
        relatorio.put("total", coletadas + distribuidas + descartadas);
        relatorio.put("vazio", coletadas == 0 && distribuidas == 0 && descartadas == 0);
        return relatorio;
    }
}
