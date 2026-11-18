package br.com.rotavital.service;

import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Doador;
import br.com.rotavital.repository.JsonDataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class DoadorService {

    private final JsonDataRepository repo;

    public DoadorService(JsonDataRepository repo) {
        this.repo = repo;
    }

    public List<Doador> listar() {
        return repo.getDoadores();
    }

    public Doador buscarPorId(String id) {
        return repo.getDoadores().stream()
                .filter(d -> d.getId().equals(id))
                .findFirst().orElse(null);
    }

    public String cadastrar(Doador doador) {
        String cpfLimpo = limparCpf(doador.getCpf());
        for (Doador d : repo.getDoadores()) {
            if (limparCpf(d.getCpf()).equals(cpfLimpo)) {
                return "CPF já cadastrado";
            }
        }
        LocalDate hoje = LocalDate.now();
        int idade = Period.between(doador.getDataNascimento(), hoje).getYears();
        if (idade < 16) {
            return "Idade mínima para doação é 16 anos";
        }
        if (idade >= 70) {
            return "Idade máxima para doação é 69 anos";
        }
        boolean primeiraDoacao = doador.getUltimaDoacao() == null;
        if (primeiraDoacao && idade >= 60) {
            return "Primeira doação não é permitida para maiores de 59 anos";
        }
        if (doador.getPeso() < 50.0) {
            return "Peso inferior ao mínimo permitido para doação";
        }

        doador.setId(UUID.randomUUID().toString());
        doador.setCpf(cpfLimpo);
        repo.getDoadores().add(doador);
        repo.gravar();
        return null;
    }

    private String limparCpf(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("[^0-9]", "");
    }

    public boolean aptoPorIntervalo(Doador doador) {
        return aptoPorIntervaloNaData(doador, LocalDate.now());
    }

    public boolean aptoPorIntervaloNaData(Doador doador, LocalDate data) {
        if (doador.getUltimaDoacao() == null) return true;
        int diasMinimos = "F".equalsIgnoreCase(doador.getSexo()) ? 90 : 60;
        return !data.isBefore(doador.getUltimaDoacao().plusDays(diasMinimos));
    }

    public LocalDate calcularProximaDataApta(Doador doador) {
        if (doador.getUltimaDoacao() == null) {
            return LocalDate.now();
        }
        int diasMinimos = "F".equalsIgnoreCase(doador.getSexo()) ? 90 : 60;
        return doador.getUltimaDoacao().plusDays(diasMinimos);
    }

    public boolean estaApto(Doador doador) {
        if (doador.getUltimaDoacao() == null) return true;
        return !LocalDate.now().isBefore(calcularProximaDataApta(doador));
    }

    public List<BolsaSangue> listarDoacoesDoador(String doadorId) {
        if (repo == null || repo.getEstoque() == null) return Collections.emptyList();
        return repo.getEstoque().paraLista().stream()
                .filter(b -> doadorId.equals(b.getDoadorId()))
                .sorted(Comparator.comparing(BolsaSangue::getDataColeta, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
}

