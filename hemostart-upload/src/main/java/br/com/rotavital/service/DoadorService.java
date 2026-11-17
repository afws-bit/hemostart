package br.com.rotavital.service;

import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.Doador;
import br.com.rotavital.repository.DoadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DoadorService {

    private final DoadorRepository repository;

    public DoadorService(DoadorRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Doador> listar() {
        return repository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Doador buscarPorId(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Doador não encontrado"));
    }

    /** HU01 — valida as regras de elegibilidade e grava o doador no banco. */
    @Transactional
    public Doador cadastrar(Doador doador) {
        String cpfLimpo = somenteDigitos(doador.getCpf());
        if (cpfLimpo.length() != 11) {
            throw new RegraNegocioException("CPF deve conter 11 dígitos");
        }
        if (repository.existsByCpf(cpfLimpo)) {
            throw new RegraNegocioException("CPF já cadastrado");
        }
        if (doador.getDataNascimento() == null) {
            throw new RegraNegocioException("Data de nascimento é obrigatória");
        }

        int idade = doador.idadeEm(LocalDate.now());
        if (idade < Doador.IDADE_MINIMA) {
            throw new RegraNegocioException("Idade mínima para doação é 16 anos");
        }
        if (idade < 18 && !doador.isAutorizacaoResponsavel()) {
            throw new RegraNegocioException("Doadores com 16 ou 17 anos precisam de autorização do responsável");
        }
        if (idade > Doador.IDADE_MAXIMA) {
            throw new RegraNegocioException("Idade máxima para doação é 69 anos");
        }
        if (doador.isPrimeiraDoacao() && idade > Doador.IDADE_MAXIMA_PRIMEIRA_DOACAO) {
            throw new RegraNegocioException("Primeira doação não é permitida para maiores de 59 anos");
        }
        if (doador.getPeso() < Doador.PESO_MINIMO) {
            throw new RegraNegocioException("Peso inferior ao mínimo permitido para doação");
        }

        doador.setCpf(cpfLimpo);
        return repository.save(doador);
    }

    public boolean aptoPorIntervalo(Doador doador) {
        return doador.aptoPorIntervalo(LocalDate.now());
    }

    private String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("[^0-9]", "");
    }
}
