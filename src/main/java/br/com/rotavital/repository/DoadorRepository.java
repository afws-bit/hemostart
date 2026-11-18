package br.com.rotavital.repository;

import br.com.rotavital.model.Doador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoadorRepository extends JpaRepository<Doador, String> {

    boolean existsByCpf(String cpf);

    List<Doador> findAllByOrderByNomeAsc();
}
