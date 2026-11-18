package br.com.rotavital.repository;

import br.com.rotavital.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, String> {

    List<Agendamento> findAllByOrderByDataAscHorarioAsc();

    /** HU11 — vagas já ocupadas num dia e horário. */
    long countByStatusAndDataAndHorario(String status, LocalDate data, String horario);
}
