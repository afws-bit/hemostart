package br.com.rotavital.repository;

import br.com.rotavital.model.Requisicao;
import br.com.rotavital.model.StatusRequisicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RequisicaoRepository extends JpaRepository<Requisicao, String> {

    List<Requisicao> findByStatusInOrderByDataSolicitacaoAsc(Collection<StatusRequisicao> status);
}
