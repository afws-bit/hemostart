package br.com.rotavital.service;

import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Doador;
import br.com.rotavital.repository.BolsaSangueRepository;
import br.com.rotavital.repository.DoadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class DoadorService {

    private final DoadorRepository repository;
    private final BolsaSangueRepository bolsaRepository;

    public DoadorService(DoadorRepository repository, BolsaSangueRepository bolsaRepository) {
        this.repository = repository;
        this.bolsaRepository = bolsaRepository;
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

    /** HU11 — o doador estará apto na data desejada? */
    public boolean aptoPorIntervaloNaData(Doador doador, LocalDate data) {
        return doador.aptoPorIntervalo(data);
    }

    /** HU14 — primeira data em que o doador pode doar de novo. */
    public LocalDate calcularProximaDataApta(Doador doador) {
        return doador.proximaDataApta(LocalDate.now());
    }

    /** HU14 */
    public boolean estaApto(Doador doador) {
        return doador.aptoPorIntervalo(LocalDate.now());
    }

    /** HU14 — doações (bolsas coletadas) do doador, da mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<BolsaSangue> listarDoacoesDoador(String doadorId) {
        return bolsaRepository.findByDoador_IdOrderByDataColetaDesc(doadorId);
    }

    private String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("[^0-9]", "");
    }
}
