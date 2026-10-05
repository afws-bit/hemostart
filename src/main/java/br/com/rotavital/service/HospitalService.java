package br.com.rotavital.service;

import br.com.rotavital.model.Hospital;
import br.com.rotavital.repository.JsonDataRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class HospitalService {

    private final JsonDataRepository repo;

    public HospitalService(JsonDataRepository repo) {
        this.repo = repo;
    }

    public List<Hospital> listar() {
        return repo.getHospitais();
    }

    public Hospital buscarPorId(String id) {
        return repo.getHospitais().stream()
                .filter(h -> h.getId().equals(id))
                .findFirst().orElse(null);
    }

    public Hospital buscarPorCnpj(String cnpj) {
        String cnpjLimpo = limparCnpj(cnpj);
        return repo.getHospitais().stream()
                .filter(h -> limparCnpj(h.getCnpj()).equals(cnpjLimpo))
                .findFirst().orElse(null);
    }

    public String cadastrar(Hospital hospital) {
        String cnpjLimpo = limparCnpj(hospital.getCnpj());
        for (Hospital h : repo.getHospitais()) {
            if (limparCnpj(h.getCnpj()).equals(cnpjLimpo)) {
                return "CNPJ já cadastrado";
            }
        }
        hospital.setId(UUID.randomUUID().toString());
        hospital.setCnpj(cnpjLimpo);
        hospital.setAtivo(true);
        repo.getHospitais().add(hospital);
        repo.gravar();
        return null;
    }

    public void alternarStatus(String id) {
        Hospital h = buscarPorId(id);
        if (h != null) {
            h.setAtivo(!h.isAtivo());
            repo.gravar();
        }
    }

    private String limparCnpj(String cnpj) {
        return cnpj == null ? "" : cnpj.replaceAll("[^0-9]", "");
    }
}
