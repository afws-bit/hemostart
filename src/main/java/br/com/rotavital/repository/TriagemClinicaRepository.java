package br.com.rotavital.repository;

import br.com.rotavital.model.TriagemClinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TriagemClinicaRepository extends JpaRepository<TriagemClinica, String> {

    List<TriagemClinica> findAllByOrderByDataHoraDesc();

    List<TriagemClinica> findByDoador_IdOrderByDataHoraDesc(String doadorId);

    /** HU12 — a triagem mais recente decide se o doador pode coletar. */
    Optional<TriagemClinica> findTopByDoador_IdOrderByDataHoraDesc(String doadorId);
}
