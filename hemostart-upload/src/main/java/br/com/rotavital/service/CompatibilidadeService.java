package br.com.rotavital.service;

import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Configuracao;
import br.com.rotavital.model.StatusBolsa;
import br.com.rotavital.model.TipoSanguineo;
import br.com.rotavital.repository.BolsaSangueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompatibilidadeService {

    private final BolsaSangueRepository bolsaRepository;
    private final ConfiguracaoService configuracaoService;

    public CompatibilidadeService(BolsaSangueRepository bolsaRepository,
                                  ConfiguracaoService configuracaoService) {
        this.bolsaRepository = bolsaRepository;
        this.configuracaoService = configuracaoService;
    }

    /** HU04 — delega a regra ABO/Rh ao enum TipoSanguineo. */
    public boolean compativel(String tipoBolsa, String tipoReceptor) {
        try {
            return TipoSanguineo.deRotulo(tipoBolsa).podeDoarPara(TipoSanguineo.deRotulo(tipoReceptor));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * HU04 — sugere bolsas compatíveis seguindo FEFO (First Expire, First Out):
     * primeiro as do mesmo tipo, depois as de outros tipos compatíveis,
     * sempre da validade mais próxima para a mais distante.
     */
    @Transactional(readOnly = true)
    public List<BolsaSangue> sugerirBolsas(String tipoReceptor, int quantidade) {
        List<BolsaSangue> disponiveis = bolsaRepository.findByStatusOrderByDataValidadeAsc(StatusBolsa.DISPONIVEL)
                .stream()
                .filter(b -> compativel(b.getTipoSanguineo(), tipoReceptor))
                .toList();

        List<BolsaSangue> selecionadas = new ArrayList<>();
        disponiveis.stream().filter(b -> b.getTipoSanguineo().equals(tipoReceptor)).forEach(selecionadas::add);
        disponiveis.stream().filter(b -> !b.getTipoSanguineo().equals(tipoReceptor)).forEach(selecionadas::add);

        return selecionadas.stream().limit(quantidade).toList();
    }

    /** HU03 + HU10 — contagem por tipo e status e alerta de estoque crítico. */
    @Transactional(readOnly = true)
    public Map<String, Object> gerarPainelEstoque() {
        List<BolsaSangue> todas = bolsaRepository.findAll();
        Configuracao config = configuracaoService.obter();

        Map<String, Map<String, Long>> painel = new LinkedHashMap<>();
        Map<String, Boolean> alertas = new LinkedHashMap<>();

        for (String tipo : TipoSanguineo.rotulos()) {
            Map<String, Long> contagem = new LinkedHashMap<>();
            for (StatusBolsa status : StatusBolsa.values()) {
                long qtd = todas.stream()
                        .filter(b -> b.getTipoSanguineo().equals(tipo) && b.getStatus() == status)
                        .count();
                contagem.put(status.getChave(), qtd);
            }
            painel.put(tipo, contagem);
            alertas.put(tipo, config.estoqueCritico(tipo, contagem.get(StatusBolsa.DISPONIVEL.getChave())));
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("painel", painel);
        resultado.put("alertas", alertas);
        return resultado;
    }
}
