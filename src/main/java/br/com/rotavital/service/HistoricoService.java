package br.com.rotavital.service;

import br.com.rotavital.estruturas.PilhaHistorico;
import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.HistoricoOperacao;
import br.com.rotavital.model.TipoOperacao;
import br.com.rotavital.repository.BolsaSangueRepository;
import br.com.rotavital.repository.HistoricoOperacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistoricoService {

    private final HistoricoOperacaoRepository historicoRepository;
    private final BolsaSangueRepository bolsaRepository;

    public HistoricoService(HistoricoOperacaoRepository historicoRepository,
                            BolsaSangueRepository bolsaRepository) {
        this.historicoRepository = historicoRepository;
        this.bolsaRepository = bolsaRepository;
    }

    @Transactional
    public HistoricoOperacao registrar(TipoOperacao tipo, String codigoBolsa, String descricao) {
        return historicoRepository.save(new HistoricoOperacao(tipo, codigoBolsa, descricao));
    }

    /** Lê o histórico do banco e o empilha (PilhaHistorico) para exibir do mais recente ao mais antigo. */
    @Transactional(readOnly = true)
    public List<HistoricoOperacao> listarHistorico() {
        PilhaHistorico<HistoricoOperacao> pilha = new PilhaHistorico<>();
        historicoRepository.findAllByOrderByDataHoraAsc().forEach(pilha::empilhar);
        return pilha.paraLista();
    }

    @Transactional(readOnly = true)
    public BolsaSangue buscarBolsa(String codigo) {
        return bolsaRepository.findById(codigo.trim()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<HistoricoOperacao> historicosDaBolsa(String codigo) {
        return historicoRepository.findByCodigoBolsaOrderByDataHoraAsc(codigo.trim());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> gerarRelatorio(LocalDate inicio, LocalDate fim,
                                              String tipoSanguineo, String hospitalId) {
        List<BolsaSangue> filtradas = bolsaRepository.findAll().stream()
                .filter(b -> filtroAtivo(tipoSanguineo) ? b.getTipoSanguineo().equals(tipoSanguineo) : true)
                .filter(b -> filtroAtivo(hospitalId) ? hospitalId.equals(b.getHospitalDestinoId()) : true)
                .toList();

        long coletadas = filtradas.stream()
                .filter(b -> dentro(b.getDataColeta(), inicio, fim)).count();
        long distribuidas = filtradas.stream()
                .filter(b -> dentro(b.getDataDistribuicao(), inicio, fim)).count();
        long descartadas = filtradas.stream()
                .filter(b -> dentro(b.getDataDescarte(), inicio, fim)).count();

        Map<String, Object> relatorio = new LinkedHashMap<>();
        relatorio.put("coletadas", coletadas);
        relatorio.put("distribuidas", distribuidas);
        relatorio.put("descartadas", descartadas);
        relatorio.put("total", coletadas + distribuidas + descartadas);
        relatorio.put("vazio", coletadas == 0 && distribuidas == 0 && descartadas == 0);
        return relatorio;
    }

    private boolean filtroAtivo(String valor) {
        return valor != null && !valor.isBlank() && !"TODOS".equals(valor);
    }

    private boolean dentro(LocalDate data, LocalDate inicio, LocalDate fim) {
        return data != null && !data.isBefore(inicio) && !data.isAfter(fim);
    }

    private boolean dentro(LocalDateTime dataHora, LocalDate inicio, LocalDate fim) {
        return dataHora != null && dentro(dataHora.toLocalDate(), inicio, fim);
    }
}
