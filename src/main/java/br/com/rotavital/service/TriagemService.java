package br.com.rotavital.service;

import br.com.rotavital.model.Doador;
import br.com.rotavital.model.TriagemClinica;
import br.com.rotavital.repository.JsonDataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class TriagemService {

    private final JsonDataRepository repo;
    private final DoadorService doadorService;

    public TriagemService(JsonDataRepository repo, DoadorService doadorService) {
        this.repo = repo;
        this.doadorService = doadorService;
    }

    public List<TriagemClinica> listarTodas() {
        return repo.getTriagens().stream()
                .sorted(Comparator.comparing(TriagemClinica::getDataHora, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public TriagemClinica buscarPorId(String id) {
        return repo.getTriagens().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst().orElse(null);
    }

    public TriagemClinica buscarUltimaPorDoador(String doadorId) {
        return repo.getTriagens().stream()
                .filter(t -> t.getDoadorId().equals(doadorId))
                .max(Comparator.comparing(TriagemClinica::getDataHora, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    public List<TriagemClinica> listarPorDoador(String doadorId) {
        return repo.getTriagens().stream()
                .filter(t -> t.getDoadorId().equals(doadorId))
                .sorted(Comparator.comparing(TriagemClinica::getDataHora, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public TriagemClinica registrarTriagem(String doadorId, double peso, String pressaoArterial,
                                          double temperatura, double hemoglobina) {
        repo.getLock().lock();
        try {
            Doador doador = doadorService.buscarPorId(doadorId);
            if (doador == null) {
                throw new IllegalArgumentException("Doador não encontrado");
            }

            TriagemClinica triagem = new TriagemClinica();
            triagem.setId(UUID.randomUUID().toString());
            triagem.setDoadorId(doadorId);
            triagem.setPeso(peso);
            triagem.setPressaoArterial(pressaoArterial);
            triagem.setTemperatura(temperatura);
            triagem.setHemoglobina(hemoglobina);
            triagem.setDataHora(LocalDateTime.now());

            boolean apto = true;
            String motivo = null;

            double hbMinima = "F".equalsIgnoreCase(doador.getSexo()) ? 12.5 : 13.0;
            if (hemoglobina < hbMinima) {
                apto = false;
                motivo = "Hemoglobina abaixo do mínimo";
            } else if (peso < 50.0) {
                apto = false;
                motivo = "Peso inferior ao mínimo permitido";
            } else if (temperatura > 37.0) {
                apto = false;
                motivo = "Temperatura corporal acima do limite permitido (37°C)";
            }

            triagem.setResultado(apto ? "apto" : "inapto");
            triagem.setMotivoInaptidao(motivo);

            repo.getTriagens().add(triagem);
            repo.gravar();
            return triagem;
        } finally {
            repo.getLock().unlock();
        }
    }
}
