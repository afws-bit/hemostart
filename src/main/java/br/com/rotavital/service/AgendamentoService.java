package br.com.rotavital.service;

import br.com.rotavital.concorrencia.SecaoCritica;
import br.com.rotavital.exception.RecursoNaoEncontradoException;
import br.com.rotavital.exception.RegraNegocioException;
import br.com.rotavital.model.Agendamento;
import br.com.rotavital.model.Doador;
import br.com.rotavital.repository.AgendamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** HU11 — agendamento de doação. */
@Service
public class AgendamentoService {

    private final AgendamentoRepository repository;
    private final DoadorService doadorService;
    private final ConfiguracaoService configuracaoService;
    private final SecaoCritica secaoCritica;

    public AgendamentoService(AgendamentoRepository repository, DoadorService doadorService,
                              ConfiguracaoService configuracaoService, SecaoCritica secaoCritica) {
        this.repository = repository;
        this.doadorService = doadorService;
        this.configuracaoService = configuracaoService;
        this.secaoCritica = secaoCritica;
    }

    @Transactional(readOnly = true)
    public List<Agendamento> listar() {
        return repository.findAllByOrderByDataAscHorarioAsc();
    }

    /** Roda na SecaoCritica: o limite de vagas por horário nunca é ultrapassado. */
    public Agendamento agendar(String doadorId, LocalDate data, String horario) {
        return secaoCritica.executar(() -> agendarExclusivo(doadorId, data, horario));
    }

    private Agendamento agendarExclusivo(String doadorId, LocalDate data, String horario) {
        Doador doador = doadorService.buscarPorId(doadorId);
        if (data == null || horario == null || horario.isBlank()) {
            throw new RegraNegocioException("Informe a data e o horário");
        }
        if (data.isBefore(LocalDate.now())) {
            throw new RegraNegocioException("Não é possível agendar em data passada");
        }
        if (!doador.aptoPorIntervalo(data)) {
            throw new RegraNegocioException("Doador ainda não apto para nova doação");
        }
        int limite = configuracaoService.obter().getLimiteVagasPorHorario();
        long ocupadas = repository.countByStatusAndDataAndHorario(Agendamento.AGENDADO, data, horario);
        if (ocupadas >= limite) {
            throw new RegraNegocioException("Horário indisponível");
        }
        return repository.save(Agendamento.novo(doador, data, horario));
    }

    @Transactional
    public void cancelar(String id) {
        Agendamento a = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado"));
        a.cancelar();
        repository.save(a);
    }
}
