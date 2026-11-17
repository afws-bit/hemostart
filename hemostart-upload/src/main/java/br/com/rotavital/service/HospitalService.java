package br.com.rotavital.service;

import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.Hospital;
import br.com.rotavital.repository.HospitalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HospitalService {

    private final HospitalRepository repository;

    public HospitalService(HospitalRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Hospital> listar() {
        return repository.findAllByOrderByRazaoSocialAsc();
    }

    @Transactional(readOnly = true)
    public List<Hospital> listarAtivos() {
        return repository.findByAtivoTrueOrderByRazaoSocialAsc();
    }

    @Transactional(readOnly = true)
    public Hospital buscarPorId(String id) {
        return repository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public Hospital buscarPorCnpj(String cnpj) {
        return repository.findByCnpj(somenteDigitos(cnpj)).orElse(null);
    }

    /** HU09 — CNPJ único; todo hospital nasce ativo. */
    @Transactional
    public Hospital cadastrar(Hospital hospital) {
        String cnpjLimpo = somenteDigitos(hospital.getCnpj());
        if (cnpjLimpo.length() != 14) {
            throw new RegraNegocioException("CNPJ deve conter 14 dígitos");
        }
        if (repository.existsByCnpj(cnpjLimpo)) {
            throw new RegraNegocioException("CNPJ já cadastrado");
        }
        hospital.setCnpj(cnpjLimpo);
        hospital.setAtivo(true);
        return repository.save(hospital);
    }

    @Transactional
    public void alternarStatus(String id) {
        Hospital h = repository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Hospital não encontrado"));
        h.alternarStatus();
        repository.save(h);
    }

    private String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("[^0-9]", "");
    }
}
