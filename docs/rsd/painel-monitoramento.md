# Painel de monitoramento de rede — HemoStart / Rota Vital

> **RSD · Unidade 2 · Critério "Painel de monitoramento" (35%)**
> Responsáveis: Elisa Martins Galindo Santos e Kelly Priscilla de Siqueira Melo · Revisão: 18/11/2026

## 1. Onde fica

- **Tela:** menu **Rede** → `/rede` (local: <http://localhost:8080/rede>; produção: <https://hemostart-rotavital-api.onrender.com/rede>)
- **Dados em JSON:** `GET /api/monitoramento/rede?janela=300` (janela em segundos, de 10 a 3600)

A tela se atualiza sozinha a cada 5 segundos e permite escolher a janela: último minuto, 5 min, 15 min ou 1 hora.

## 2. O que o painel mostra

Cada **seta do diagrama de arquitetura** que passa pela aplicação vira um card com seis métricas e uma situação colorida:

| Card | Ligação no diagrama | Como é medida |
|---|---|---|
| **Cliente → Aplicação** | Proxy/borda → Aplicação, HTTP/1.1 · 8080 | Um filtro (`MetricasHttpFilter`) cronometra cada requisição recebida, do primeiro ao último byte processado pelo servidor |
| **Aplicação → Banco** | Aplicação → PostgreSQL, TCP · 5432 | Uma sonda (`SondaBanco`) executa `SELECT 1` a cada 5 s e mede a ida e volta, com timeout de 2 s |
| **Benchmark externo** | Internet → borda, HTTPS · 443 | Resultado importado do [script de benchmarking](benchmarking.md) (essa ligação acontece fora do processo Java) |

Métricas de cada card:

| Métrica | Definição |
|---|---|
| Latência média, p50, p95, p99 | Média e percentis (posto mais próximo) das medições bem-sucedidas da janela |
| Jitter | Média de \|Lᵢ − Lᵢ₋₁\| entre medições consecutivas: quanto a latência "balança" |
| Perda | Medições que falharam ÷ total × 100. HTTP: respostas 5xx. Banco: timeout ou erro de conexão |
| Vazão | Medições por segundo na janela (para HTTP, requisições/s atendidas) |
| Situação | **OK**, **Atenção** ou **Crítico** pelas metas do [plano de implantação, seção 5](plano-implantacao-migracao.md#5-metas-de-desempenho-critérios-de-aceite) |

Além dos cards:

- **Gráfico de tempo de resposta HTTP**: últimas 60 requisições, com as falhas em vermelho.
- **Gráfico da sonda do banco**: últimas 60 medições (5 min com o intervalo padrão).
- **Tráfego por rota**: requisições, média, p95 e falhas por rota; identificadores viram `{id}` (ex.: `GET /api/v1/bolsas/{id}`).
- **Benchmark externo**: tabela com DNS, ping, TCP, TLS, HTTPS por endpoint e vazão sob carga.

## 3. Como funciona por dentro

```
requisição ──► MetricasHttpFilter ──► controllers ──► resposta
                    │ (tempo, status, rota)
                    ▼
              MonitorRede ◄── SondaBanco (SELECT 1 a cada 5 s)
   (buffers circulares: 5.000 requisições, 720 sondas)
                    │
                    ▼
     GET /api/monitoramento/rede ──► tela /rede (Chart.js)
```

| Classe | Papel |
|---|---|
| `monitoramento.MetricasHttpFilter` | Filtro servlet; ignora arquivos estáticos e o próprio endpoint do painel, para não medir a si mesmo |
| `monitoramento.SondaBanco` | Tarefa `@Scheduled`; intervalo em `monitoramento.sonda.intervalo.ms` (padrão 5000) |
| `monitoramento.SerieMedicoes` | Buffer circular *thread-safe*: memória constante, descarta as medições mais antigas |
| `monitoramento.EstatisticasRede` | Média, percentil, jitter, perda e vazão; mesmas fórmulas do script de benchmark |
| `monitoramento.MonitorRede` | Junta tudo, aplica as metas e monta o `PainelRedeDTO` |
| `controller.MonitoramentoController` | `GET /rede` (tela) e `GET /api/monitoramento/rede` (JSON) |

O destino do banco mostrado no card vem da URL JDBC, **sem usuário nem senha** (são removidos antes de exibir).

## 4. Importar o resultado do benchmark externo

1. Rode o script (instruções em [benchmarking.md](benchmarking.md)).
2. Copie o arquivo `benchmark-externo.json` gerado para `src/main/resources/static/rede/benchmark-externo.json`.
3. Faça o commit. Depois do próximo deploy, o card **Benchmark externo** mostra os números.

## 5. Testes automatizados

| Teste | O que garante |
|---|---|
| `MonitoramentoRedeTest` (5 testes) | Percentis, jitter, perda, vazão e buffer circular, com valores calculados à mão |
| `monitoramento.MonitorRedeTest` (4 testes) | Agrupamento de rotas, URL do banco sem senha, regras OK/Atenção/Crítico, cálculo das duas ligações |
| `PainelRedeTest` (3 testes) | Ponta a ponta com Spring: o filtro registra as requisições, a sonda mede o banco, o JSON traz as duas ligações e o painel não mede a si mesmo |

## 6. Limitações conhecidas

- As medições ficam **em memória**: recomeçam quando a aplicação reinicia (no plano gratuito do Render, quando o serviço dorme).
- O card Cliente → Aplicação mede o **tempo no servidor**. O tempo que o usuário sente inclui a rede até a borda e o TLS, que o benchmark externo mede.
- Com mais de uma instância da aplicação, cada uma teria o seu painel. Na arquitetura-alvo, a evolução natural é exportar as métricas para Prometheus/Grafana, como linha pontilhada de observabilidade no diagrama.

## 7. Evidência

Print da tela `/rede` com o sistema em uso: `docs/img/painel-rede.png` (tirar depois do deploy, com algumas telas abertas antes, para haver tráfego).
