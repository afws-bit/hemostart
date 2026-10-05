package br.com.rotavital.service;

import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.Doador;
import br.com.rotavital.model.HistoricoOperacao;
import br.com.rotavital.repository.JsonDataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class EstoqueService {

    private final JsonDataRepository repo;
    private final Comparator<BolsaSangue> porValidade = Comparator.comparing(BolsaSangue::getDataValidade);

    public EstoqueService(JsonDataRepository repo) {
        this.repo = repo;
    }

    public String registrarColeta(String doadorId, String componente, int volume) {
        repo.getLock().lock();
        try {
            DoadorService doadorService = new DoadorService(repo);
            Doador doador = doadorService.buscarPorId(doadorId);
            if (doador == null) return "Doador não encontrado";
            if (!doadorService.aptoPorIntervalo(doador)) {
                return "Doador ainda não apto para nova doação";
            }

            BolsaSangue bolsa = new BolsaSangue();
            bolsa.setCodigo(repo.gerarCodigoBolsa());
            bolsa.setTipoSanguineo(doador.getTipoSanguineo());
            bolsa.setComponente(componente);
            bolsa.setVolume(volume);
            bolsa.setDoadorId(doadorId);
            bolsa.setDataColeta(LocalDate.now());
            bolsa.setDataValidade(LocalDate.now().plusDays(repo.validadeDias(componente)));
            bolsa.setStatus("disponivel");

            repo.getEstoque().inserirOrdenado(bolsa, porValidade);
            doador.setUltimaDoacao(LocalDate.now());

            HistoricoOperacao h = new HistoricoOperacao(
                    "COLETA", bolsa.getCodigo(),
                    "Coleta registrada para " + doador.getNome() + " - " + componente
            );
            repo.getHistoricoStack().empilhar(h);
            repo.gravar();
            return null;
        } finally {
            repo.getLock().unlock();
        }
    }

    public List<BolsaSangue> listarEstoque() {
        return repo.getEstoque().paraLista();
    }

    public BolsaSangue buscarPorCodigo(String codigo) {
        return repo.getEstoque().buscar(b -> b.getCodigo().equals(codigo));
    }

    public void verificarValidade() {
        repo.getLock().lock();
        try {
            LocalDate hoje = LocalDate.now();
            List<BolsaSangue> bolsas = repo.getEstoque().paraLista();
            for (BolsaSangue b : bolsas) {
                if ("disponivel".equals(b.getStatus()) || "reservada".equals(b.getStatus())) {
                    if (!b.getDataValidade().isAfter(hoje)) {
                        b.setStatus("vencida");
                        b.setUsoPrioritario(false);
                        HistoricoOperacao h = new HistoricoOperacao(
                                "VENCIMENTO", b.getCodigo(),
                                "Bolsa vencida em " + b.getDataValidade()
                        );
                        repo.getHistoricoStack().empilhar(h);
                    } else if ("disponivel".equals(b.getStatus())) {
                        long diasRestantes = hoje.until(b.getDataValidade()).getDays();
                        b.setUsoPrioritario(diasRestantes <= 5);
                    }
                }
            }
            repo.gravar();
        } finally {
            repo.getLock().unlock();
        }
    }

    public String descartar(String codigo, String responsavel) {
        repo.getLock().lock();
        try {
            BolsaSangue b = buscarPorCodigo(codigo);
            if (b == null) return "Bolsa não encontrada";
            if ("descartada".equals(b.getStatus())) return "Bolsa já foi descartada";
            b.setStatus("descartada");
            b.setDataDescarte(java.time.LocalDateTime.now());
            b.setResponsavelDescarte(responsavel);
            b.setUsoPrioritario(false);
            HistoricoOperacao h = new HistoricoOperacao(
                    "DESCARTE", b.getCodigo(),
                    "Descartada por " + responsavel
            );
            repo.getHistoricoStack().empilhar(h);
            repo.gravar();
            return null;
        } finally {
            repo.getLock().unlock();
        }
    }

    public List<BolsaSangue> bolsasVencidas() {
        return repo.getEstoque().paraLista().stream()
                .filter(b -> "vencida".equals(b.getStatus()))
                .toList();
    }

    public List<BolsaSangue> bolsasUsoPrioritario() {
        return repo.getEstoque().paraLista().stream()
                .filter(b -> b.isUsoPrioritario() && "disponivel".equals(b.getStatus()))
                .toList();
    }
}
