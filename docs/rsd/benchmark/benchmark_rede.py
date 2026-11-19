#!/usr/bin/env python3
"""
Benchmarking de rede do HemoStart / Rota Vital (RSD, Unidade 2).

Mede, de fora para dentro, a ligação Internet -> borda HTTPS/443 -> aplicação:
  - latência (média, p50, p95, p99, mín., máx.)
  - jitter (média de |L(i) - L(i-1)| entre medições consecutivas)
  - perda (% de tentativas sem resposta válida: timeout, erro de conexão ou HTTP 5xx)
  - vazão (requisições/s e KB/s com 1, 5 e 10 clientes simultâneos)

Camadas medidas, da mais baixa para a mais alta:
  DNS -> ICMP (ping) -> conexão TCP :443 -> handshake TLS -> HTTPS GET por endpoint -> carga.

Só usa a biblioteca padrão do Python 3.8+ (nada para instalar).

Uso (na pasta deste arquivo):
    python benchmark_rede.py
    python benchmark_rede.py --origem "Wi-Fi de casa, Recife"
    python benchmark_rede.py --url http://localhost:8080 --origem "mesma máquina"

Saída em resultados/AAAA-MM-DD_HHMM/:
    amostras.csv            uma linha por medição (dados brutos)
    resumo.json             métricas calculadas
    relatorio.md            tabelas prontas para o documento
    benchmark-externo.json  cópia do resumo para o painel /rede da aplicação
"""

import argparse
import csv
import http.client
import json
import math
import os
import platform
import re
import shutil
import socket
import ssl
import statistics
import subprocess
import sys
import threading
import time
from datetime import datetime
from urllib.parse import urlparse

URL_PADRAO = "https://hemostart-rotavital-api.onrender.com"
ENDPOINTS_PADRAO = ["/", "/api/v1/estoque", "/api/v1/solicitacoes", "/api/v1/bolsas/alertas-validade"]
ENDPOINT_CARGA = "/api/v1/estoque"

# Metas do plano de implantação (docs/rsd/plano-implantacao-migracao.md, seção 5)
META_HTTPS_P95_MS = 800
META_PERDA_PCT = 1.0
META_VAZAO_REQ_S = 10.0


# ----------------------------------------------------------------- estatística

def percentil(valores, p):
    """Posto mais próximo: posição = teto(p/100 * n), a partir de 1 na lista ordenada."""
    if not valores:
        return None
    ordenados = sorted(valores)
    posicao = max(1, math.ceil(p / 100.0 * len(ordenados)))
    return ordenados[posicao - 1]


def jitter(latencias_em_ordem):
    if len(latencias_em_ordem) < 2:
        return 0.0
    difs = [abs(b - a) for a, b in zip(latencias_em_ordem, latencias_em_ordem[1:])]
    return sum(difs) / len(difs)


def resumir(amostras):
    """amostras: lista de (latencia_ms ou None, sucesso)."""
    lat = [ms for ms, ok in amostras if ok and ms is not None]
    total = len(amostras)
    if total == 0:
        return None
    r = {
        "amostras": total,
        "sucessos": len(lat),
        "perda_pct": round(100.0 * (total - len(lat)) / total, 2),
        "media_ms": None, "p50_ms": None, "p95_ms": None, "p99_ms": None,
        "min_ms": None, "max_ms": None, "desvio_ms": None, "jitter_ms": None,
    }
    if lat:
        r.update({
            "media_ms": round(statistics.mean(lat), 2),
            "p50_ms": round(percentil(lat, 50), 2),
            "p95_ms": round(percentil(lat, 95), 2),
            "p99_ms": round(percentil(lat, 99), 2),
            "min_ms": round(min(lat), 2),
            "max_ms": round(max(lat), 2),
            "desvio_ms": round(statistics.stdev(lat), 2) if len(lat) > 1 else 0.0,
            "jitter_ms": round(jitter(lat), 2),
        })
    return r


# ----------------------------------------------------------------- registro

class Registro:
    def __init__(self):
        self.linhas = []
        self.trava = threading.Lock()

    def add(self, tipo, alvo, indice, ms, ok, detalhe=""):
        with self.trava:
            self.linhas.append({
                "tipo": tipo, "alvo": alvo, "indice": indice,
                "instante": datetime.now().isoformat(timespec="milliseconds"),
                "latencia_ms": "" if ms is None else round(ms, 3),
                "sucesso": int(bool(ok)), "detalhe": detalhe,
            })


def info(msg):
    try:
        print(msg, flush=True)
    except UnicodeEncodeError:  # console do Windows sem UTF-8
        print(msg.encode("ascii", "replace").decode(), flush=True)


def progresso(i, total):
    if total >= 10 and (i + 1) % max(1, total // 10) == 0:
        print(".", end="", flush=True)


# ----------------------------------------------------------------- medições

def medir_cold_start(alvo, reg):
    """Primeira requisição: no plano gratuito do Render o serviço dorme e pode levar ~30-60 s para acordar."""
    inicio = time.perf_counter()
    try:
        con = nova_conexao(alvo, timeout=120)
        con.request("GET", "/", headers={"User-Agent": "hemostart-benchmark"})
        resp = con.getresponse()
        resp.read()
        ms = (time.perf_counter() - inicio) * 1000
        ok = resp.status < 500
        con.close()
        reg.add("cold_start", "/", 0, ms, ok, "HTTP %d" % resp.status)
        return round(ms, 1), resp.status
    except Exception as e:  # noqa: BLE001
        ms = (time.perf_counter() - inicio) * 1000
        reg.add("cold_start", "/", 0, ms, False, type(e).__name__)
        return round(ms, 1), None


def medir_dns(host, n, reg):
    amostras = []
    for i in range(n):
        inicio = time.perf_counter()
        try:
            socket.getaddrinfo(host, None)
            ms = (time.perf_counter() - inicio) * 1000
            amostras.append((ms, True))
            reg.add("dns", host, i, ms, True)
        except OSError as e:
            amostras.append((None, False))
            reg.add("dns", host, i, None, False, str(e))
        time.sleep(0.05)
    return resumir(amostras)


def medir_icmp(host, n, reg):
    """Usa o comando ping do sistema. Alguns provedores bloqueiam ICMP: nesse caso retorna None."""
    if not shutil.which("ping"):
        return None, "comando ping não encontrado"
    windows = platform.system().lower().startswith("win")
    cmd = ["ping", "-n" if windows else "-c", str(n), host]
    try:
        saida = subprocess.run(cmd, capture_output=True, text=True, timeout=n * 3 + 15,
                               errors="replace").stdout
    except (subprocess.TimeoutExpired, OSError) as e:
        return None, type(e).__name__
    tempos = re.findall(r"(?:time|tempo)\s*[=<]\s*([\d.,]+)\s*ms", saida, flags=re.IGNORECASE)
    tempos = [float(t.replace(",", ".")) for t in tempos]
    for i, t in enumerate(tempos):
        reg.add("icmp", host, i, t, True)
    perdidos = max(0, n - len(tempos))
    for i in range(perdidos):
        reg.add("icmp", host, len(tempos) + i, None, False, "sem resposta")
    if not tempos:
        return None, "sem respostas ICMP (provavelmente bloqueado pelo provedor)"
    return resumir([(t, True) for t in tempos] + [(None, False)] * perdidos), None


def medir_tcp_tls(host, porta, n, usar_tls, reg):
    tcp, tls = [], []
    contexto = ssl.create_default_context() if usar_tls else None
    for i in range(n):
        inicio = time.perf_counter()
        try:
            s = socket.create_connection((host, porta), timeout=5)
            ms_tcp = (time.perf_counter() - inicio) * 1000
            tcp.append((ms_tcp, True))
            reg.add("tcp", "%s:%d" % (host, porta), i, ms_tcp, True)
            if usar_tls:
                inicio_tls = time.perf_counter()
                try:
                    ss = contexto.wrap_socket(s, server_hostname=host)
                    ms_tls = (time.perf_counter() - inicio_tls) * 1000
                    tls.append((ms_tls, True))
                    reg.add("tls", host, i, ms_tls, True, ss.version() or "")
                    ss.close()
                except (OSError, ssl.SSLError) as e:
                    tls.append((None, False))
                    reg.add("tls", host, i, None, False, type(e).__name__)
                    s.close()
            else:
                s.close()
        except OSError as e:
            tcp.append((None, False))
            reg.add("tcp", "%s:%d" % (host, porta), i, None, False, type(e).__name__)
        time.sleep(0.2)
    return resumir(tcp), (resumir(tls) if usar_tls else None)


def nova_conexao(alvo, timeout=15):
    if alvo.scheme == "https":
        return http.client.HTTPSConnection(alvo.hostname, alvo.port or 443, timeout=timeout,
                                           context=ssl.create_default_context())
    return http.client.HTTPConnection(alvo.hostname, alvo.port or 80, timeout=timeout)


def um_get(con, caminho):
    """Retorna (ms_total, ms_primeiro_byte, status, bytes). Lança exceção em erro de rede."""
    inicio = time.perf_counter()
    con.request("GET", caminho, headers={"User-Agent": "hemostart-benchmark", "Accept": "*/*"})
    resp = con.getresponse()
    ttfb = (time.perf_counter() - inicio) * 1000
    corpo = resp.read()
    total = (time.perf_counter() - inicio) * 1000
    return total, ttfb, resp.status, len(corpo)


def medir_http(alvo, caminho, n, reg):
    """Conexão persistente (keep-alive), como um navegador: mede só a requisição, não o handshake."""
    amostras, ttfbs = [], []
    con = nova_conexao(alvo)
    for i in range(n):
        try:
            total, ttfb, status, _ = um_get(con, caminho)
            ok = status < 500
            amostras.append((total, ok))
            if ok:
                ttfbs.append(ttfb)
            reg.add("http", caminho, i, total, ok, "HTTP %d" % status)
        except (OSError, http.client.HTTPException) as e:
            amostras.append((None, False))
            reg.add("http", caminho, i, None, False, type(e).__name__)
            con.close()
            con = nova_conexao(alvo)
        progresso(i, n)
        time.sleep(0.2)
    con.close()
    r = resumir(amostras)
    if r is not None:
        r["endpoint"] = caminho
        r["ttfb_medio_ms"] = round(statistics.mean(ttfbs), 2) if ttfbs else None
    return r


def medir_vazao(alvo, caminho, concorrencia, duracao, reg):
    resultados = []
    trava = threading.Lock()
    fim = time.perf_counter() + duracao

    def cliente(id_cliente):
        con = nova_conexao(alvo)
        i = 0
        while time.perf_counter() < fim:
            try:
                total, _, status, tamanho = um_get(con, caminho)
                ok = status < 500
                with trava:
                    resultados.append((total, ok, tamanho if ok else 0))
                reg.add("vazao_c%d" % concorrencia, caminho, i, total, ok, "cliente %d HTTP %d" % (id_cliente, status))
            except (OSError, http.client.HTTPException) as e:
                with trava:
                    resultados.append((None, False, 0))
                reg.add("vazao_c%d" % concorrencia, caminho, i, None, False, "cliente %d %s" % (id_cliente, type(e).__name__))
                con.close()
                con = nova_conexao(alvo)
            i += 1
        con.close()

    inicio = time.perf_counter()
    threads = [threading.Thread(target=cliente, args=(c,)) for c in range(concorrencia)]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    decorrido = time.perf_counter() - inicio

    r = resumir([(ms, ok) for ms, ok, _ in resultados]) or {"amostras": 0, "sucessos": 0, "perda_pct": 0.0}
    sucessos = sum(1 for _, ok, _ in resultados if ok)
    bytes_ok = sum(b for _, _, b in resultados)
    r.update({
        "concorrencia": concorrencia,
        "duracao_s": round(decorrido, 2),
        "requisicoes": len(resultados),
        "req_por_s": round(sucessos / decorrido, 2),
        "kb_por_s": round(bytes_ok / 1024 / decorrido, 2),
    })
    return r


# ----------------------------------------------------------------- relatório

def fmt(v, casas=1, sufixo=""):
    if v is None:
        return "—"
    return ("{:,.%df}" % casas).format(v).replace(",", "X").replace(".", ",").replace("X", ".") + sufixo


def linha(nome, m):
    if not m:
        return "| %s | — | — | — | — | — | — |" % nome
    return "| %s | %s | %s | %s | %s | %s | %d/%d |" % (
        nome, fmt(m["media_ms"]), fmt(m["p50_ms"]), fmt(m["p95_ms"]), fmt(m["jitter_ms"]),
        fmt(m["perda_pct"], 2, "%"), m["sucessos"], m["amostras"])


def observacoes(res):
    obs = []
    cs = res.get("cold_start_ms")
    if cs and cs > 10000:
        obs.append("A primeira requisição levou %s s: o serviço estava dormindo (plano gratuito do Render) e "
                   "precisou acordar. Esse tempo foi separado das demais medições." % fmt(cs / 1000, 1))
    tcp, tls = res.get("tcp"), res.get("tls")
    if tcp and tcp.get("media_ms") and tls and tls.get("media_ms"):
        obs.append("Abrir uma conexão nova custa em média %s ms (TCP %s + TLS %s). Por isso navegadores e o "
                   "script reaproveitam a conexão (keep-alive)." % (
                       fmt(tcp["media_ms"] + tls["media_ms"]), fmt(tcp["media_ms"]), fmt(tls["media_ms"])))
    icmp = res.get("icmp")
    if icmp and icmp.get("media_ms") and tcp and tcp.get("media_ms"):
        obs.append("Ping (ICMP) médio de %s ms e conexão TCP de %s ms: os dois medem basicamente o RTT até a "
                   "borda; a diferença vem do processamento do SYN na borda." % (fmt(icmp["media_ms"]), fmt(tcp["media_ms"])))
    elif res.get("icmp_motivo"):
        obs.append("ICMP: %s. A latência de rede foi estimada pela conexão TCP." % res["icmp_motivo"])
    for h in res.get("http", []):
        if h and h.get("p95_ms") is not None:
            sit = "dentro" if h["p95_ms"] <= META_HTTPS_P95_MS else "acima"
            obs.append("`GET %s`: p95 de %s ms, %s da meta de %d ms." % (h["endpoint"], fmt(h["p95_ms"]), sit, META_HTTPS_P95_MS))
    perdas = [h["perda_pct"] for h in res.get("http", []) if h]
    if perdas:
        maior = max(perdas)
        obs.append("Maior perda entre os endpoints: %s (meta ≤ %s)." % (fmt(maior, 2, "%"), fmt(META_PERDA_PCT, 0, "%")))
    vz = res.get("vazao", [])
    if len(vz) >= 2 and vz[0]["req_por_s"] > 0:
        ganho = vz[-1]["req_por_s"] / vz[0]["req_por_s"]
        obs.append("Com %d clientes a vazão foi %sx a de 1 cliente (%s → %s req/s) e o p95 foi de %s para %s ms." % (
            vz[-1]["concorrencia"], fmt(ganho, 1), fmt(vz[0]["req_por_s"], 2), fmt(vz[-1]["req_por_s"], 2),
            fmt(vz[0].get("p95_ms")), fmt(vz[-1].get("p95_ms"))))
    return obs


def gerar_markdown(res):
    p = res["parametros"]
    md = []
    md.append("# Resultado do benchmarking de rede — HemoStart\n")
    md.append("- **Alvo:** `%s`" % res["alvo"])
    md.append("- **Data:** %s" % res["gerado_em"])
    md.append("- **Origem da medição:** %s" % res["origem"])
    md.append("- **Sistema:** %s" % res["sistema"])
    md.append("- **Parâmetros:** %d amostras por medição, intervalo de 200 ms, carga de %d s por nível em `%s`\n" % (
        p["amostras"], p["duracao_carga_s"], p["endpoint_carga"]))
    md.append("## Latência, jitter e perda\n")
    md.append("| Medição | Média (ms) | p50 (ms) | p95 (ms) | Jitter (ms) | Perda | Sucessos |")
    md.append("|---|---|---|---|---|---|---|")
    md.append(linha("Resolução DNS", res.get("dns")))
    md.append(linha("ICMP ping", res.get("icmp")))
    md.append(linha("Conexão TCP :%d" % res["porta"], res.get("tcp")))
    md.append(linha("Handshake TLS", res.get("tls")))
    for h in res.get("http", []):
        md.append(linha("HTTPS GET `%s`" % h["endpoint"], h))
    md.append("")
    if res.get("cold_start_ms") is not None:
        md.append("Primeira requisição (cold start): **%s ms** (fora das estatísticas acima).\n" % fmt(res["cold_start_ms"], 0))
    md.append("## Vazão sob carga\n")
    md.append("| Clientes simultâneos | Requisições | Req/s | KB/s | Média (ms) | p95 (ms) | Perda |")
    md.append("|---|---|---|---|---|---|---|")
    for v in res.get("vazao", []):
        md.append("| %d | %d | %s | %s | %s | %s | %s |" % (
            v["concorrencia"], v["requisicoes"], fmt(v["req_por_s"], 2), fmt(v["kb_por_s"]),
            fmt(v.get("media_ms")), fmt(v.get("p95_ms")), fmt(v["perda_pct"], 2, "%")))
    md.append("\n## Observações automáticas\n")
    for o in observacoes(res):
        md.append("- " + o)
    md.append("\n_Definições: latência = tempo até a resposta completa; jitter = média de |Lᵢ − Lᵢ₋₁|; "
              "perda = tentativas sem resposta válida (timeout, erro de conexão ou HTTP 5xx) ÷ total; "
              "percentis pelo posto mais próximo._\n")
    return "\n".join(md)


# ----------------------------------------------------------------- principal

def main():
    ap = argparse.ArgumentParser(description="Benchmarking de rede do HemoStart (RSD).")
    ap.add_argument("--url", default=URL_PADRAO, help="endereço da aplicação (padrão: deploy no Render)")
    ap.add_argument("--origem", default="", help='de onde está medindo, ex.: "Wi-Fi de casa, Recife"')
    ap.add_argument("--amostras", type=int, default=50, help="medições por teste (padrão 50)")
    ap.add_argument("--duracao", type=int, default=15, help="segundos de carga por nível (padrão 15)")
    ap.add_argument("--concorrencias", default="1,5,10", help="clientes simultâneos na carga (padrão 1,5,10)")
    ap.add_argument("--saida", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "resultados"))
    args = ap.parse_args()

    alvo = urlparse(args.url.rstrip("/"))
    if alvo.scheme not in ("http", "https") or not alvo.hostname:
        sys.exit("URL inválida: %s" % args.url)
    porta = alvo.port or (443 if alvo.scheme == "https" else 80)
    n = max(5, args.amostras)
    concorrencias = [int(c) for c in args.concorrencias.split(",") if c.strip()]
    reg = Registro()

    info("Benchmark de rede HemoStart -> %s" % args.url)
    info("[1/7] Acordando o servidor (primeira requisição, pode levar até 1 min no Render)...")
    cold_ms, cold_status = medir_cold_start(alvo, reg)
    info("      %s ms (HTTP %s)" % (fmt(cold_ms, 0), cold_status))
    if cold_status is None or cold_status >= 500:
        sys.exit("O servidor não respondeu. Confira a URL e tente de novo.")

    info("[2/7] DNS..."); dns = medir_dns(alvo.hostname, min(n, 20), reg)
    info("[3/7] Ping (ICMP)..."); icmp, icmp_motivo = medir_icmp(alvo.hostname, min(n, 20), reg)
    info("[4/7] Conexão TCP e handshake TLS...")
    tcp, tls = medir_tcp_tls(alvo.hostname, porta, min(n, 30), alvo.scheme == "https", reg)
    info("[5/7] HTTPS GET por endpoint")
    http_res = []
    for caminho in ENDPOINTS_PADRAO:
        print("      %-36s" % caminho, end="", flush=True)
        r = medir_http(alvo, caminho, n, reg)
        http_res.append(r)
        info(" média %s ms, p95 %s ms, perda %s" % (fmt(r["media_ms"]), fmt(r["p95_ms"]), fmt(r["perda_pct"], 2, "%")))
    info("[6/7] Vazão sob carga em %s" % ENDPOINT_CARGA)
    vazao = []
    for c in concorrencias:
        v = medir_vazao(alvo, ENDPOINT_CARGA, c, args.duracao, reg)
        vazao.append(v)
        info("      %2d cliente(s): %s req/s, p95 %s ms, perda %s" % (c, fmt(v["req_por_s"], 2), fmt(v.get("p95_ms")), fmt(v["perda_pct"], 2, "%")))
        time.sleep(2)

    info("[7/7] Gravando resultados...")
    agora = datetime.now()
    res = {
        "gerado_em": agora.strftime("%d/%m/%Y %H:%M"),
        "alvo": args.url,
        "porta": porta,
        "origem": args.origem or "não informada",
        "sistema": "%s %s, Python %s" % (platform.system(), platform.release(), platform.python_version()),
        "parametros": {"amostras": n, "duracao_carga_s": args.duracao, "concorrencias": concorrencias,
                       "endpoint_carga": ENDPOINT_CARGA},
        "cold_start_ms": cold_ms,
        "dns": dns, "icmp": icmp, "icmp_motivo": icmp_motivo, "tcp": tcp, "tls": tls,
        "http": http_res, "vazao": vazao,
    }
    pasta = os.path.join(args.saida, agora.strftime("%Y-%m-%d_%H%M"))
    os.makedirs(pasta, exist_ok=True)
    with open(os.path.join(pasta, "amostras.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["tipo", "alvo", "indice", "instante", "latencia_ms", "sucesso", "detalhe"])
        w.writeheader()
        w.writerows(reg.linhas)
    for nome in ("resumo.json", "benchmark-externo.json"):
        with open(os.path.join(pasta, nome), "w", encoding="utf-8") as f:
            json.dump(res, f, ensure_ascii=False, indent=2)
    with open(os.path.join(pasta, "relatorio.md"), "w", encoding="utf-8") as f:
        f.write(gerar_markdown(res))
    info("\nPronto! Arquivos em: %s" % pasta)
    info("Mande a pasta inteira (ou os arquivos relatorio.md e benchmark-externo.json) para o grupo.")


if __name__ == "__main__":
    main()
