package br.com.rotavital.service;

import br.com.rotavital.model.Doador;
import br.com.rotavital.model.TriagemClinica;
import br.com.rotavital.repository.TriagemClinicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** HU12 — triagem clínica pré-doação. */
@Service
public class TriagemService {

    private final TriagemClinicaRepository repository;
    private final DoadorService doadorService;

    public TriagemService(TriagemClinicaRepository repository, DoadorService doadorService) {
        this.repository = repository;
        this.doadorService = doadorService;
    }

    @Transactional(readOnly = true)
    public List<TriagemClinica> listarTodas() {
        return repository.findAllByOrderByDataHoraDesc();
    }

    @Transactional(readOnly = true)
    public TriagemClinica buscarUltimaPorDoador(String doadorId) {
        return repository.findTopByDoador_IdOrderByDataHoraDesc(doadorId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<TriagemClinica> listarPorDoador(String doadorId) {
        return repository.findByDoador_IdOrderByDataHoraDesc(doadorId);
    }

    @Transactional
    public TriagemClinica registrarTriagem(String doadorId, double peso, String pressaoArterial,
                                          double temperatura, double hemoglobina) {
        Doador doador = doadorService.buscarPorId(doadorId);
        return repository.save(TriagemClinica.avaliar(doador, peso, pressaoArterial, temperatura, hemoglobina));
    }
}
