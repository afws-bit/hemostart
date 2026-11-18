package br.com.rotavital.repository;

import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.StatusBolsa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BolsaSangueRepository extends JpaRepository<BolsaSangue, String> {

    List<BolsaSangue> findByStatusOrderByDataValidadeAsc(StatusBolsa status);

    List<BolsaSangue> findByStatusInOrderByDataValidadeAsc(Collection<StatusBolsa> status);

    List<BolsaSangue> findByStatusAndUsoPrioritarioTrueOrderByDataValidadeAsc(StatusBolsa status);

    List<BolsaSangue> findByRequisicao_IdAndStatus(String requisicaoId, StatusBolsa status);

    long countByTipoSanguineoAndStatus(String tipoSanguineo, StatusBolsa status);

    Optional<BolsaSangue> findTopByOrderByCodigoDesc();

    /** HU14 — doações de um doador, da mais recente para a mais antiga. */
    List<BolsaSangue> findByDoador_IdOrderByDataColetaDesc(String doadorId);
}
