package br.com.rotavital.repository;

import br.com.rotavital.model.HistoricoOperacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoOperacaoRepository extends JpaRepository<HistoricoOperacao, String> {

    List<HistoricoOperacao> findAllByOrderByDataHoraAsc();

    List<HistoricoOperacao> findByCodigoBolsaOrderByDataHoraAsc(String codigoBolsa);
}
