"""
Gerador de carga do HemoStart para a demonstração de observabilidade.

Roda em loop dentro do docker-compose e exercita a aplicação como um dia de trabalho no banco de sangue:
  - consultas (estoque, fila de pedidos, alertas de validade, compatibilidade, relatórios, telas web);
  - cadastro de doador + coleta (gera a métrica hemostart_bolsas_coletadas_total e um trace com vários spans);
  - pedido de hospital -> aprovação (timer hemostart_alocacao_duracao) -> distribuição;
  - recusas de pedido;
  - casos de erro: 404 (bolsa inexistente), 403 (hospital inativo), 400 (tipo inválido) e 500 (/api/teste/erro).

Só biblioteca padrão. Variáveis: ALVO (padrão http://hemostart:8080) e INTERVALO_S (padrão 2).
"""

import json
import os
import random
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import date

ALVO = os.environ.get("ALVO", "http://hemostart:8080").rstrip("/")
INTERVALO = float(os.environ.get("INTERVALO_S", "2"))

TIPOS = ["O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"]
PESOS_TIPOS = [36, 9, 34, 8, 8, 2, 2, 1]          # distribuição aproximada na população brasileira
COMPONENTES = ["Concentrado de Hemácias", "Plaquetas", "Plasma"]
HOSPITAIS_ATIVOS = ["12345678000195", "98765432000188"]   # cadastrados pelos dados iniciais
HOSPITAL_INATIVO = "11122233000177"
NOMES = ["Ana", "Bruno", "Carla", "Diego", "Elisa", "Fábio", "Gabriela", "Heitor", "Isabela", "João",
         "Kelly", "Lucas", "Mariana", "Nicolas", "Olívia", "Paulo", "Rafaela", "Sérgio", "Tatiane", "Vitor"]
SOBRENOMES = ["Silva", "Santos", "Oliveira", "Souza", "Lima", "Pereira", "Costa", "Rodrigues", "Almeida", "Melo"]

contagem = {"ok": 0, "erro_esperado": 0, "falha": 0}


def chamar(metodo, caminho, corpo=None, esperado=None):
    """Faz a requisição e devolve (status, json ou None). Nunca lança exceção."""
    dados = json.dumps(corpo).encode() if corpo is not None else None
    req = urllib.request.Request(ALVO + caminho, data=dados, method=metodo,
                                 headers={"Content-Type": "application/json", "Accept": "application/json, text/html",
                                          "User-Agent": "hemostart-gerador-carga"})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            status, texto = resp.status, resp.read()
    except urllib.error.HTTPError as e:
        status, texto = e.code, e.read()
    except (urllib.error.URLError, OSError) as e:
        contagem["falha"] += 1
        print(f"[carga] sem resposta em {metodo} {caminho}: {e}")
        return None, None
    if esperado and status in esperado:
        contagem["erro_esperado" if status >= 400 else "ok"] += 1
    elif status < 400:
        contagem["ok"] += 1
    else:
        contagem["erro_esperado"] += 1
    try:
        return status, json.loads(texto) if texto else None
    except ValueError:
        return status, None


def cpf_aleatorio():
    return "".join(str(random.randint(0, 9)) for _ in range(11))


def consultas():
    tipo = random.choices(TIPOS, PESOS_TIPOS)[0]
    for caminho in ("/api/v1/estoque", "/api/v1/solicitacoes", "/api/v1/bolsas/alertas-validade",
                    f"/api/v1/compatibilidade?tipo={urllib.parse.quote(tipo)}&quantidade=2",
                    "/api/v1/relatorios/doacoes", "/", "/estoque", "/requisicoes"):
        chamar("GET", caminho)


def doacao():
    """Cadastra um doador e registra a coleta: controller -> serviço -> seção crítica -> repositórios."""
    hoje = date.today()
    nascimento = date(hoje.year - random.randint(20, 55), random.randint(1, 12), random.randint(1, 28))
    doador = {
        "nome": f"{random.choice(NOMES)} {random.choice(SOBRENOMES)}",
        "cpf": cpf_aleatorio(),
        "dataNascimento": nascimento.isoformat(),
        "sexo": random.choice("MF"),
        "tipoSanguineo": random.choices(TIPOS, PESOS_TIPOS)[0],
        "peso": random.randint(58, 98),
        "contato": "(81) 90000-0000",
        "historicoSaude": "Sem restrições (dado sintético)",
        "autorizacaoResponsavel": False,
    }
    status, d = chamar("POST", "/api/v1/doadores", doador)
    if status == 201 and d and d.get("id"):
        chamar("POST", "/api/v1/coletas",
               {"doadorId": d["id"], "componente": random.choice(COMPONENTES), "volume": 450})


def pedido_hospital():
    """Pedido -> aprovação (reserva FEFO + rota) -> distribuição. Sem estoque compatível, a aprovação dá 409."""
    pedido = {"cnpjHospital": random.choice(HOSPITAIS_ATIVOS),
              "tipoSanguineo": random.choices(TIPOS, PESOS_TIPOS)[0],
              "quantidade": 1, "urgente": random.random() < 0.3}
    status, s = chamar("POST", "/api/v1/solicitacoes", pedido)
    if status != 201 or not s:
        return
    if random.random() < 0.15:
        chamar("POST", f"/api/v1/solicitacoes/{s['id']}/recusa", {"motivo": "Pedido duplicado (simulação)"})
        return
    status, _ = chamar("POST", f"/api/v1/solicitacoes/{s['id']}/aprovacao", esperado={200, 409})
    if status == 200:
        chamar("POST", "/api/v1/distribuicoes", {"solicitacaoId": s["id"], "responsavel": "gerador-de-carga"})


def erros_de_negocio():
    chamar("GET", "/api/v1/bolsas/BS-99999", esperado={404})
    chamar("POST", "/api/v1/solicitacoes", {"cnpjHospital": HOSPITAL_INATIVO, "tipoSanguineo": "O-",
                                            "quantidade": 1, "urgente": True}, esperado={403})
    chamar("GET", "/api/v1/compatibilidade?tipo=XYZ", esperado={400})


def erro_interno():
    chamar("GET", "/api/teste/erro", esperado={500})


def esperar_aplicacao():
    for _ in range(120):
        status, _ = chamar("GET", "/api/v1/estoque")
        if status == 200:
            return
        time.sleep(2)


def main():
    print(f"[carga] alvo {ALVO}, intervalo {INTERVALO}s")
    esperar_aplicacao()
    ciclo = 0
    while True:
        ciclo += 1
        consultas()
        if ciclo % 2 == 0:
            doacao()
        if ciclo % 3 == 0:
            pedido_hospital()
        if ciclo % 7 == 0:
            erros_de_negocio()
        if ciclo % 15 == 0:
            erro_interno()
        if ciclo % 30 == 0:
            print(f"[carga] ciclo {ciclo}: {contagem}")
        time.sleep(INTERVALO * random.uniform(0.6, 1.4))


if __name__ == "__main__":
    main()
