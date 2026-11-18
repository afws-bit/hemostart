package br.com.rotavital.service;

import br.com.rotavital.model.Configuracao;
import br.com.rotavital.model.TipoSanguineo;
import br.com.rotavital.repository.ConfiguracaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ConfiguracaoService {

    private final ConfiguracaoRepository repository;

    public ConfiguracaoService(ConfiguracaoRepository repository) {
        this.repository = repository;
    }

    /** Lê a configuração do banco; se ainda não existir, cria com os valores padrão. */
    @Transactional
    public Configuracao obter() {
        return repository.findById(Configuracao.ID_UNICO)
                .orElseGet(() -> repository.save(new Configuracao()));
    }

    /** Recebe os campos do formulário (minimo_Apos, minimo_Oneg...) e grava os novos níveis. */
    @Transactional
    public Configuracao salvarNiveis(Map<String, String> params) {
        Configuracao config = obter();
        for (String tipo : TipoSanguineo.rotulos()) {
            String chave = "minimo_" + tipo.replace("+", "pos").replace("-", "neg");
            String valor = params.get(chave);
            if (valor != null && !valor.isBlank()) {
                try {
                    config.definirMinimo(tipo, Integer.parseInt(valor.trim()));
                } catch (IllegalArgumentException ignorado) {
                    // valor inválido/negativo: mantém o anterior
                }
            }
        }
        return repository.save(config);
    }

    @Transactional
    public void definirMinimo(String tipo, int minimo) {
        Configuracao config = obter();
        config.definirMinimo(tipo, minimo);
        repository.save(config);
    }
}
