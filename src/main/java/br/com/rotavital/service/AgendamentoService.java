package br.com.rotavital.service;

import br.com.rotavital.model.Agendamento;
import br.com.rotavital.model.Doador;
import br.com.rotavital.repository.JsonDataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class AgendamentoService {

    private final JsonDataRepository repo;
    private final DoadorService doadorService;

    public AgendamentoService(JsonDataRepository repo, DoadorService doadorService) {
        this.repo = repo;
        this.doadorService = doadorService;
    }

    public List<Agendamento> listar() {
        return repo.getAgendamentos().stream()
                .sorted(Comparator.comparing(Agendamento::getData, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Agendamento::getHorario, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public Agendamento buscarPorId(String id) {
        return repo.getAgendamentos().stream()
                .filter(a -> a.getId().equals(id))
                .findFirst().orElse(null);
    }

    public String agendar(String doadorId, LocalDate data, String horario) {
        repo.getLock().lock();
        try {
            Doador doador = doadorService.buscarPorId(doadorId);
            if (doador == null) {
                return "Doador não encontrado";
            }

            if (!doadorService.aptoPorIntervaloNaData(doador, data)) {
                return "Doador ainda não apto para nova doação";
            }

            int limiteVagas = repo.getConfiguracao().getLimiteVagasPorHorario();
            long ocupadas = repo.getAgendamentos().stream()
                    .filter(a -> "agendado".equals(a.getStatus()))
                    .filter(a -> a.getData() != null && a.getData().equals(data))
                    .filter(a -> a.getHorario() != null && a.getHorario().equals(horario))
                    .count();

            if (ocupadas >= limiteVagas) {
                return "Horário indisponível";
            }

            Agendamento agendamento = new Agendamento();
            agendamento.setId(UUID.randomUUID().toString());
            agendamento.setDoadorId(doadorId);
            agendamento.setData(data);
            agendamento.setHorario(horario);
            agendamento.setStatus("agendado");
            agendamento.setDataCriacao(LocalDateTime.now());

            repo.getAgendamentos().add(agendamento);
            repo.gravar();
            return null;
        } finally {
            repo.getLock().unlock();
        }
    }

    public String cancelar(String id) {
        repo.getLock().lock();
        try {
            Agendamento a = buscarPorId(id);
            if (a == null) {
                return "Agendamento não encontrado";
            }
            a.setStatus("cancelado");
            repo.gravar();
            return null;
        } finally {
            repo.getLock().unlock();
        }
    }
}
