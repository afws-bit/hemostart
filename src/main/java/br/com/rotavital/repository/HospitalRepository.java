package br.com.rotavital.repository;

import br.com.rotavital.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, String> {

    boolean existsByCnpj(String cnpj);

    Optional<Hospital> findByCnpj(String cnpj);

    List<Hospital> findAllByOrderByRazaoSocialAsc();

    List<Hospital> findByAtivoTrueOrderByRazaoSocialAsc();
}
