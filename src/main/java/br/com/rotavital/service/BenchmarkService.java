package br.com.rotavital.service;

import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.repository.JsonDataRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

@Service
public class BenchmarkService {

    private final JsonDataRepository repo;

    public BenchmarkService(JsonDataRepository repo) {
        this.repo = repo;
    }

    public Map<String, Object> executarBenchmark() throws InterruptedException {
        int massaSize = 2_000_000;
        int warmupRuns = 2;
        int measureRuns = 3;

        List<BolsaSangue> massa = gerarMassaSintetica(massaSize);

        int[] configs = {1, 2, 4, 8};
        Map<String, Object> resultado = new LinkedHashMap<>();
        List<Map<String, Object>> linhas = new ArrayList<>();

        long tempoSequencial = 0;

        for (int threads : configs) {
            // warmup
            for (int i = 0; i < warmupRuns; i++) {
                executar(massa, threads);
            }
            // measure
            long total = 0;
            for (int i = 0; i < measureRuns; i++) {
                long inicio = System.currentTimeMillis();
                executar(massa, threads);
                total += System.currentTimeMillis() - inicio;
            }
            long media = total / measureRuns;
            if (threads == 1) tempoSequencial = media;

            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("threads", threads);
            linha.put("tempoMedio", media);
            double speedup = threads == 1 ? 1.0 : (double) tempoSequencial / media;
            linha.put("speedup", String.format("%.2f", speedup));
            linha.put("eficiencia", String.format("%.2f", speedup / threads));
            linhas.add(linha);
        }

        resultado.put("linhas", linhas);
        resultado.put("processadores", Runtime.getRuntime().availableProcessors());
        resultado.put("massa", massaSize);
        return resultado;
    }

    private Map<String, Long> executar(List<BolsaSangue> massa, int nThreads) throws InterruptedException {
        if (nThreads == 1) {
            return classificarSequencial(massa);
        }
        return classificarParalelo(massa, nThreads);
    }

    private Map<String, Long> classificarSequencial(List<BolsaSangue> massa) {
        Map<String, Long> contadores = new HashMap<>();
        java.time.LocalDate hoje = java.time.LocalDate.now();
        for (BolsaSangue b : massa) {
            String chave = b.getTipoSanguineo() + "_" + classificar(b, hoje);
            contadores.merge(chave, 1L, Long::sum);
        }
        return contadores;
    }

    private Map<String, Long> classificarParalelo(List<BolsaSangue> massa, int nThreads) throws InterruptedException {
        ExecutorService exec = Executors.newFixedThreadPool(nThreads);
        int chunk = massa.size() / nThreads;
        List<Future<Map<String, Long>>> futures = new ArrayList<>();
        java.time.LocalDate hoje = java.time.LocalDate.now();

        for (int i = 0; i < nThreads; i++) {
            int from = i * chunk;
            int to = (i == nThreads - 1) ? massa.size() : from + chunk;
            List<BolsaSangue> parte = massa.subList(from, to);
            futures.add(exec.submit(() -> {
                Map<String, Long> local = new HashMap<>();
                for (BolsaSangue b : parte) {
                    String chave = b.getTipoSanguineo() + "_" + classificar(b, hoje);
                    local.merge(chave, 1L, Long::sum);
                }
                return local;
            }));
        }

        Map<String, Long> total = new HashMap<>();
        for (Future<Map<String, Long>> f : futures) {
            try {
                f.get().forEach((k, v) -> total.merge(k, v, Long::sum));
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        exec.shutdown();
        return total;
    }

    private String classificar(BolsaSangue b, java.time.LocalDate hoje) {
        if (b.getDataValidade().isBefore(hoje)) return "vencida";
        long dias = hoje.until(b.getDataValidade()).getDays();
        if (dias <= 5) return "uso_prioritario";
        return "disponivel";
    }

    private List<BolsaSangue> gerarMassaSintetica(int n) {
        String[] tipos = {"A+","A-","B+","B-","AB+","AB-","O+","O-"};
        String[] comps = {"Concentrado de Hemácias","Plaquetas","Plasma"};
        int[] validades = {42, 5, 365};
        java.time.LocalDate base = java.time.LocalDate.now().minusDays(10);

        List<BolsaSangue> lista = new ArrayList<>(n);
        Random rng = new Random(42);
        for (int i = 0; i < n; i++) {
            BolsaSangue b = new BolsaSangue();
            b.setTipoSanguineo(tipos[i % tipos.length]);
            int ci = rng.nextInt(comps.length);
            b.setComponente(comps[ci]);
            b.setDataColeta(base);
            int offset = rng.nextInt(60) - 15;
            b.setDataValidade(base.plusDays(validades[ci] + offset));
            b.setStatus("disponivel");
            lista.add(b);
        }
        return lista;
    }
}
