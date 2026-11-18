package br.com.rotavital.config;

import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Componente;
import br.com.rotavital.model.Doador;
import br.com.rotavital.model.Hospital;
import br.com.rotavital.model.StatusBolsa;
import br.com.rotavital.repository.BolsaSangueRepository;
import br.com.rotavital.repository.DoadorRepository;
import br.com.rotavital.repository.HospitalRepository;
import br.com.rotavital.service.ConfiguracaoService;
import br.com.rotavital.service.EstoqueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Popula o banco com dados de demonstração na primeira execução (banco vazio).
 * Desligue com rotavital.dados-iniciais=false.
 */
@Component
@ConditionalOnProperty(name = "rotavital.dados-iniciais", havingValue = "true", matchIfMissing = true)
public class DadosIniciais implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private final DoadorRepository doadorRepository;
    private final HospitalRepository hospitalRepository;
    private final BolsaSangueRepository bolsaRepository;
    private final EstoqueService estoqueService;
    private final ConfiguracaoService configuracaoService;

    public DadosIniciais(DoadorRepository doadorRepository, HospitalRepository hospitalRepository,
                         BolsaSangueRepository bolsaRepository, EstoqueService estoqueService,
                         ConfiguracaoService configuracaoService) {
        this.doadorRepository = doadorRepository;
        this.hospitalRepository = hospitalRepository;
        this.bolsaRepository = bolsaRepository;
        this.estoqueService = estoqueService;
        this.configuracaoService = configuracaoService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        configuracaoService.obter();
        if (doadorRepository.count() > 0 || hospitalRepository.count() > 0) {
            return;
        }
        log.info("Banco vazio: inserindo dados de demonstração do Rota Vital");

        Doador d1 = doadorRepository.save(doador("Carlos Alberto Silva", "12345678901",
                LocalDate.of(1985, 3, 15), "M", "O+", 78.0, "(11) 98765-4321", "Sem comorbidades.", null));
        Doador d2 = doadorRepository.save(doador("Maria Fernanda Costa", "98765432100",
                LocalDate.of(1992, 7, 22), "F", "A-", 62.0, "(11) 91234-5678", "Hipertensão controlada.", null));
        Doador d3 = doadorRepository.save(doador("João Paulo Mendes", "11122233344",
                LocalDate.of(1978, 11, 5), "M", "B+", 85.0, "(21) 99876-5432", "Sem restrições.",
                LocalDate.now().minusDays(100)));

        hospitalRepository.save(hospital("Hospital Santa Cruz", "12345678000195",
                "Rua das Flores, 100, São Paulo/SP", "(11) 3333-4444", true));
        hospitalRepository.save(hospital("Hospital Municipal Norte", "98765432000188",
                "Av. Brasil, 500, Rio de Janeiro/RJ", "(21) 2222-3333", true));
        hospitalRepository.save(hospital("Clínica São Lucas (Inativa)", "11122233000177",
                "Rua do Comércio, 200, Belo Horizonte/MG", "(31) 4444-5555", false));

        LocalDate hoje = LocalDate.now();
        bolsa(d1, Componente.CONCENTRADO_HEMACIAS, 450, hoje.minusDays(5));
        bolsa(d1, Componente.CONCENTRADO_HEMACIAS, 450, hoje.minusDays(10));
        bolsa(d2, Componente.PLAQUETAS, 200, hoje.minusDays(3));
        bolsa(d3, Componente.PLASMA, 300, hoje.minusDays(30));
        bolsa(d3, Componente.CONCENTRADO_HEMACIAS, 450, hoje.minusDays(20));
        bolsa(d1, Componente.CONCENTRADO_HEMACIAS, 450, hoje.minusDays(38)); // vence em 4 dias
        BolsaSangue vencida = bolsa(d2, Componente.PLAQUETAS, 200, hoje.minusDays(10));
        vencida.setStatus(StatusBolsa.VENCIDA);
        bolsaRepository.save(vencida);

        estoqueService.verificarValidade();
    }

    private BolsaSangue bolsa(Doador d, Componente c, int volume, LocalDate coleta) {
        return bolsaRepository.save(BolsaSangue.coletar(estoqueService.gerarCodigoBolsa(), d, c, volume, coleta));
    }

    private Doador doador(String nome, String cpf, LocalDate nasc, String sexo, String tipo, double peso,
                          String contato, String historico, LocalDate ultimaDoacao) {
        Doador d = new Doador();
        d.setNome(nome);
        d.setCpf(cpf);
        d.setDataNascimento(nasc);
        d.setSexo(sexo);
        d.setTipoSanguineo(tipo);
        d.setPeso(peso);
        d.setContato(contato);
        d.setHistoricoSaude(historico);
        d.setUltimaDoacao(ultimaDoacao);
        return d;
    }

    private Hospital hospital(String razao, String cnpj, String endereco, String contato, boolean ativo) {
        Hospital h = new Hospital();
        h.setRazaoSocial(razao);
        h.setCnpj(cnpj);
        h.setEndereco(endereco);
        h.setContato(contato);
        h.setAtivo(ativo);
        return h;
    }
}
