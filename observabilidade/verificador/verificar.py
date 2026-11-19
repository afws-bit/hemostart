"""
Verificador do ambiente de observabilidade do HemoStart.

Roda dentro da rede do Docker (docker compose run --rm verificador) e confere, item por item, o checklist
do projeto de avaliação. Grava em docs/evidencias/:
  verificacao.txt          resultado de cada item (OK / FALHOU e o motivo)
  actuator-prometheus.txt  trecho de /actuator/prometheus com as métricas da aplicação e de negócio
  prometheus-targets.txt   estado de cada alvo do Prometheus
  links-para-prints.md     links diretos do Grafana já com a consulta pronta para cada print

Nenhuma senha é gravada nos arquivos.
"""

import base64
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime

SAIDA = os.environ.get("SAIDA", "/evidencias")
GRAFANA_PUBLICO = os.environ.get("GRAFANA_PUBLICO", "http://localhost:3000")
ADMIN = (os.environ.get("GRAFANA_ADMIN_USER", "admin"), os.environ.get("GRAFANA_ADMIN_PASSWORD", ""))
VIEWER = (os.environ.get("GRAFANA_VIEWER_USER", "professor"), os.environ.get("GRAFANA_VIEWER_PASSWORD", ""))
ALVOS_ESPERADOS = ["hemostart", "node-exporter", "cadvisor", "postgres-exporter", "loki", "tempo", "alloy",
                   "prometheus"]
METRICAS_NEGOCIO = ["hemostart_bolsas_coletadas_total", "hemostart_solicitacoes_criadas_total",
                    "hemostart_solicitacoes_decididas_total", "hemostart_alocacao_duracao_seconds",
                    "hemostart_estoque_disponivel", "hemostart_solicitacoes_pendentes"]

APP_MGMT = os.environ.get("URL_ACTUATOR", "http://hemostart:8081")
APP = os.environ.get("URL_APP", "http://hemostart:8080")
PROM = os.environ.get("URL_PROMETHEUS", "http://prometheus:9090")
GRAFANA = os.environ.get("URL_GRAFANA", "http://grafana:3000")
LOKI = os.environ.get("URL_LOKI", "http://loki:3100")
TEMPO = os.environ.get("URL_TEMPO", "http://tempo:3200")

resultados = []


def http(url, auth=None, timeout=10):
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    if auth:
        token = base64.b64encode(f"{auth[0]}:{auth[1]}".encode()).decode()
        req.add_header("Authorization", "Basic " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except (urllib.error.URLError, OSError) as e:
        return None, str(e)


def item(codigo, descricao, ok, detalhe=""):
    resultados.append((codigo, descricao, ok, detalhe))
    marca = "OK     " if ok else "FALHOU "
    print(f"[{marca}] {codigo} {descricao}" + (f" — {detalhe}" if detalhe else ""), flush=True)


def gravar(nome, texto):
    with open(os.path.join(SAIDA, nome), "w", encoding="utf-8") as f:
        f.write(texto)


def link_explore(datasource_uid, tipo, consulta, extra=None):
    q = {"refId": "A", "datasource": {"type": tipo, "uid": datasource_uid}}
    q.update(consulta)
    painel = {"a": {"datasource": datasource_uid, "queries": [q], "range": {"from": "now-15m", "to": "now"}}}
    if extra:
        painel["a"].update(extra)
    return (f"{GRAFANA_PUBLICO}/explore?schemaVersion=1&orgId=1&panes="
            + urllib.parse.quote(json.dumps(painel, separators=(",", ":"))))


def contar_spans(obj):
    if isinstance(obj, dict):
        total = len(obj["spans"]) if isinstance(obj.get("spans"), list) else 0
        return total + sum(contar_spans(v) for k, v in obj.items() if k != "spans")
    if isinstance(obj, list):
        return sum(contar_spans(v) for v in obj)
    return 0


def nomes_spans(obj, acc):
    if isinstance(obj, dict):
        if isinstance(obj.get("spans"), list):
            acc.extend(s.get("name", "?") for s in obj["spans"])
        for k, v in obj.items():
            if k != "spans":
                nomes_spans(v, acc)
    elif isinstance(obj, list):
        for v in obj:
            nomes_spans(v, acc)
    return acc


def main():
    os.makedirs(SAIDA, exist_ok=True)
    print(f"Verificando o ambiente ({datetime.now():%d/%m/%Y %H:%M:%S})\n", flush=True)
    links = {}

    # E1 — Actuator
    status, corpo = http(APP_MGMT + "/actuator/prometheus", timeout=15)
    ok = status == 200 and 'application="hemostart"' in corpo
    item("E1", "/actuator/prometheus responde com a tag application=\"hemostart\"", ok,
         "" if ok else f"HTTP {status}: {corpo[:120]}")
    if status == 200:
        linhas = corpo.splitlines()
        escolhidas = [l for l in linhas if l.startswith(("hemostart_", "http_server_requests_seconds_count",
                                                          "jvm_memory_used_bytes", "hikaricp_connections_active",
                                                          "process_uptime_seconds"))]
        gravar("actuator-prometheus.txt",
               f"# GET http://hemostart:8081/actuator/prometheus  ({datetime.now():%d/%m/%Y %H:%M:%S})\n"
               f"# {len(linhas)} linhas no total; abaixo, as da aplicação e de negócio\n\n" + "\n".join(escolhidas[:120]))
        faltando = [m for m in METRICAS_NEGOCIO if m not in corpo]
        item("+1", "métricas de negócio expostas", not faltando,
             "faltando: " + ", ".join(faltando) if faltando else f"{len(METRICAS_NEGOCIO)} métricas encontradas")
        for padrao in ("http_server_requests_seconds", "jvm_memory_used_bytes", "jvm_gc_pause", "jvm_threads",
                       "process_cpu_usage", "process_uptime_seconds", "hikaricp_connections"):
            if padrao not in corpo:
                item("R2", f"métrica padrão {padrao}", False, "ausente")
                break
        else:
            item("R2", "métricas padrão: HTTP, JVM (heap, GC, threads), CPU, uptime e HikariCP", True)

    status, _ = http(APP + "/actuator/prometheus")
    item("R5", "/actuator/prometheus NÃO está na porta pública 8080", status != 200,
         f"porta 8080 respondeu HTTP {status}")

    # E2/E3 — Prometheus
    status, corpo = http(PROM + "/api/v1/targets")
    estado = {}
    if status == 200:
        for t in json.loads(corpo)["data"]["activeTargets"]:
            estado[t["labels"]["job"]] = (t["health"], t.get("lastError", ""))
        texto = "\n".join(f"{job:20s} {h.upper():5s} {erro}" for job, (h, erro) in sorted(estado.items()))
        gravar("prometheus-targets.txt", f"# Prometheus /api/v1/targets ({datetime.now():%d/%m/%Y %H:%M:%S})\n\n{texto}\n")
    for job in ALVOS_ESPERADOS:
        h, erro = estado.get(job, ("ausente", ""))
        item("E2/E3", f"alvo {job} UP no Prometheus", h == "up", "" if h == "up" else f"{h} {erro}")

    status, corpo = http(PROM + "/api/v1/query?query=" + urllib.parse.quote(
        'sum(rate(http_server_requests_seconds_count{application="hemostart"}[1m]))'))
    tem_trafego = status == 200 and json.loads(corpo)["data"]["result"]
    item("E4", "PromQL com dados (requisições/s da aplicação)", bool(tem_trafego),
         "" if tem_trafego else "sem tráfego ainda: espere o gerador de carga rodar 1–2 min")

    # E3/E4 — Grafana
    status, corpo = http(GRAFANA + "/api/datasources", auth=ADMIN)
    if status == 200:
        uids = {d["uid"] for d in json.loads(corpo)}
        item("E3", "datasources provisionados (Prometheus, Loki, Tempo)", {"prometheus", "loki", "tempo"} <= uids,
             ", ".join(sorted(uids)))
    else:
        item("E3", "login de admin no Grafana", False, f"HTTP {status}: confira GRAFANA_ADMIN_PASSWORD")
    status, corpo = http(GRAFANA + "/api/search?type=dash-db", auth=ADMIN)
    if status == 200:
        titulos = [d["title"] for d in json.loads(corpo)]
        item("E4", "dashboards de aplicação e de infraestrutura carregados", len(titulos) >= 2, "; ".join(titulos))
        for d in json.loads(corpo):
            links["dash-" + d["uid"]] = GRAFANA_PUBLICO + d["url"]

    # E7 — usuário Viewer
    status, corpo = http(GRAFANA + "/api/user/orgs", auth=VIEWER)
    papel = None
    if status == 200:
        orgs = json.loads(corpo)
        papel = orgs[0].get("role") if orgs else None
    item("E7", f"usuário '{VIEWER[0]}' entra no Grafana com papel Viewer", papel == "Viewer",
         f"papel: {papel}" if status == 200 else f"HTTP {status}")

    # E5 — Loki
    agora_ns = int(time.time() * 1e9)
    consulta = '{application="hemostart"}'
    status, corpo = http(LOKI + "/loki/api/v1/query_range?limit=20&start=%d&end=%d&query=%s" % (
        agora_ns - 15 * 60 * 10**9, agora_ns, urllib.parse.quote(consulta)))
    linhas_log = []
    if status == 200:
        for s in json.loads(corpo)["data"]["result"]:
            linhas_log += [v[1] for v in s["values"]]
    item("E5", "logs da aplicação no Loki ({application=\"hemostart\"})", bool(linhas_log),
         f"{len(linhas_log)} linhas nos últimos 15 min" if linhas_log else f"HTTP {status} {corpo[:120]}")
    com_trace = [l for l in linhas_log if "[hemostart," in l and ",," not in l.split("]")[0]]
    item("R6", "linhas de log com traceId (para navegar até o Tempo)", bool(com_trace))

    # E6 — Tempo
    trace_id, spans, nomes = None, 0, []
    traceql = '{resource.service.name="hemostart" && name=~".*coletas.*"}'
    status, corpo = http(TEMPO + "/api/search?limit=10&q=" + urllib.parse.quote(traceql))
    if status != 200 or not json.loads(corpo).get("traces"):
        status, corpo = http(TEMPO + "/api/search?limit=20&tags=" + urllib.parse.quote("service.name=hemostart"))
    if status == 200:
        for t in json.loads(corpo).get("traces", []):
            s2, c2 = http(TEMPO + f"/api/traces/{t['traceID']}")
            if s2 == 200:
                n = contar_spans(json.loads(c2))
                if n > spans:
                    trace_id, spans, nomes = t["traceID"], n, nomes_spans(json.loads(c2), [])
            if spans >= 5:
                break
    item("E6", "trace da aplicação no Tempo com vários spans", spans >= 3,
         f"trace {trace_id} com {spans} spans: " + ", ".join(nomes[:8]) if trace_id else f"HTTP {status} {corpo[:120]}")

    # Links prontos para os prints
    links["loki"] = link_explore("loki", "loki", {"expr": '{application="hemostart", level=~"WARN|ERROR"}', "queryType": "range"})
    links["loki-todos"] = link_explore("loki", "loki", {"expr": '{application="hemostart"}', "queryType": "range"})
    if trace_id:
        links["trace"] = link_explore("tempo", "tempo", {"queryType": "traceql", "query": trace_id})
        links["log-do-trace"] = link_explore("loki", "loki", {"expr": '{application="hemostart"} |= "%s"' % trace_id,
                                                              "queryType": "range"})
    links["promql"] = link_explore("prometheus", "prometheus", {
        "expr": 'histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{application="hemostart"}[5m])))',
        "range": True})
    links["negocio"] = link_explore("prometheus", "prometheus", {
        "expr": 'sum by (tipo) (rate(hemostart_bolsas_coletadas_total[5m])) * 60', "range": True})

    md = [f"# Links para os prints ({datetime.now():%d/%m/%Y %H:%M})\n",
          "Abra cada link logado como admin, espere carregar e tire o print com o relógio visível.\n",
          "| # | Print | Abrir |", "|---|---|---|",
          "| 1 | `docker compose ps` | no terminal |",
          "| 2 | /actuator/prometheus | terminal: `docker compose exec prometheus wget -qO- http://hemostart:8081/actuator/prometheus \\| grep hemostart_` |",
          "| 3 | Targets todos UP | http://localhost:9090/targets |",
          f"| 4 | Consulta PromQL (p95) | [abrir]({links['promql']}) |",
          f"| 5 | Dashboard de aplicação | [abrir]({links.get('dash-hemostart-aplicacao', GRAFANA_PUBLICO)}) |",
          f"| 6 | Métricas de negócio | role o dashboard de aplicação até \"Negócio\" ou [PromQL]({links['negocio']}) |",
          f"| 7 | Dashboard de infraestrutura | [abrir]({links.get('dash-hemostart-infraestrutura', GRAFANA_PUBLICO)}) |",
          f"| 8 | LogQL no Explore | [abrir]({links['loki']}) |"]
    if trace_id:
        md += [f"| 9 | Trace aberto no Tempo ({spans} spans) | [abrir]({links['trace']}) |",
               f"| 10 | Log → trace | [abrir os logs desse trace]({links['log-do-trace']}), expandir uma linha e clicar em **Abrir trace no Tempo** |"]
    else:
        md += ["| 9 | Trace | Explore → Tempo → Search → Service Name = hemostart |",
               "| 10 | Log → trace | Explore → Loki, expandir uma linha e clicar em **Abrir trace no Tempo** |"]
    md += ["| 11 | Acesso remoto | abrir a URL do túnel numa janela anônima e entrar com o usuário Viewer |",
           "| 12 | Código das métricas | `src/main/java/br/com/rotavital/observabilidade/MetricasNegocio.java` |"]
    gravar("links-para-prints.md", "\n".join(md) + "\n")

    falhas = [r for r in resultados if not r[2]]
    texto = [f"Verificação do ambiente de observabilidade — {datetime.now():%d/%m/%Y %H:%M:%S}", ""]
    texto += [f"[{'OK' if ok else 'FALHOU'}] {c} {d}" + (f" — {det}" if det else "") for c, d, ok, det in resultados]
    texto += ["", f"{len(resultados) - len(falhas)} de {len(resultados)} itens OK."]
    gravar("verificacao.txt", "\n".join(texto) + "\n")
    print(f"\n{len(resultados) - len(falhas)} de {len(resultados)} itens OK.")
    print("Arquivos gravados em observabilidade/docs/evidencias/ (abra links-para-prints.md para tirar os prints).")
    raise SystemExit(1 if falhas else 0)


if __name__ == "__main__":
    main()
