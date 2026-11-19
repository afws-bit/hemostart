# Plano de implantação e migração — HemoStart / Rota Vital

> **RSD · Unidade 2 · Critério "Plano de implantação/migração" (25%)**
> Responsáveis: Elisa Martins Galindo Santos e Kelly Priscilla de Siqueira Melo · Revisão: 18/11/2026
> Documentos relacionados: [diagrama de arquitetura](../arquitetura/rsd/arquitetura-hemostart-martins-melo.pdf) ·
> [benchmarking](benchmarking.md) · [painel de monitoramento](painel-monitoramento.md)

## 1. Objetivo e escopo

Este plano descreve **como o HemoStart chegou ao ambiente atual** e **como sai dele para a arquitetura-alvo** desenhada
no diagrama de RSD (VPC com sub-redes pública, de aplicação e de dados). Ele cobre:

- a **Migração 1**, já realizada: persistência em arquivos JSON → PostgreSQL 15;
- a **Migração 2**, planejada: hospedagem gerenciada no Render → rede própria (VPC), com proxy reverso na borda;
- as **metas de desempenho** que decidem se cada etapa deu certo, medidas pelo [script de benchmarking](benchmark/benchmark_rede.py)
  e pelo [painel /rede](painel-monitoramento.md);
- o **plano de volta (rollback)**, os riscos e o checklist de corte.

Fora do escopo: autenticação (ainda "a definir" no contrato da API), alta disponibilidade com várias zonas e
dados reais de pacientes (o projeto usa apenas dados sintéticos, por causa da LGPD).

## 2. Situação atual (as-is)

| Item | Como está hoje |
|---|---|
| Aplicação | Monolito Spring Boot 3.2.5 / Java 17, imagem Docker multi-stage (`Dockerfile`), escuta HTTP/8080 |
| Hospedagem | Render, *Web Service* a partir da imagem Docker: <https://hemostart-rotavital-api.onrender.com/> |
| Borda | Borda TLS do Render: recebe HTTPS/443 da internet e repassa HTTP para a porta do container |
| Banco | PostgreSQL 15 gerenciado no Render, acessado pela aplicação via JDBC (TCP/5432) |
| Configuração | Variáveis `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (sem senha no código) |
| Ambiente local | `docker compose up -d --build` (containers `rotavital_api` e `rotavital_db`, rede `rotavital_network`, volume `db_data`) ou H2 em arquivo |
| Integração contínua | GitHub Actions: `mvn clean test` + `docker build` a cada push no `master` |
| Monitoramento | Painel `/rede` (latência, jitter, perda e vazão das ligações) e logs da aplicação |

Limitações do ambiente atual que motivam a Migração 2:

1. **Plano gratuito do Render dorme** após inatividade; a primeira requisição pode levar dezenas de segundos (o benchmark mede isso como *cold start*).
2. **Sem isolamento de rede controlado pelo grupo**: não dá para provar a regra "banco só aceita a sub-rede de aplicação" do diagrama.
3. **Uma instância só**, sem como colocar um proxy próprio, limitar taxa ou terminar TLS com certificado do grupo.

## 3. Arquitetura-alvo (to-be)

É a do diagrama de arquitetura (seções 5 e 6 do documento de RSD):

| Sub-rede | CIDR | Componentes | Entrada permitida | Saída permitida |
|---|---|---|---|---|
| `sub-publica-hemostart` | 10.0.1.0/24 | Proxy reverso Nginx (termina TLS) | 0.0.0.0/0 na 443 | 10.0.10.0/24 na 8080 |
| `sub-app-hemostart` | 10.0.10.0/24 | Aplicação HemoStart (container) | 10.0.1.0/24 na 8080 | 10.0.20.0/24 na 5432 |
| `sub-dados-hemostart` | 10.0.20.0/24 | PostgreSQL 15 | 10.0.10.0/24 na 5432 | nenhuma |

Ligações: Internet → Nginx `HTTPS/443` · Nginx → aplicação `HTTP/1.1 8080` · aplicação → PostgreSQL `TCP/5432`.

## 4. Migração 1 (realizada) — arquivos JSON → PostgreSQL

**Antes:** `JsonDataRepository` lia os arquivos `data/*.json` na inicialização e regravava a cada alteração.
No diagrama da revisão 1 isso aparecia como um volume de rede NFSv4/2049 na sub-rede de dados.

**Depois:** Spring Data JPA/Hibernate com 8 tabelas (`doador`, `hospital`, `bolsa_sangue`, `requisicao`,
`agendamento`, `triagem_clinica`, `historico_operacao`, `configuracao`) em PostgreSQL 15 (H2 local e nos testes).

| Passo | O que foi feito | Evidência |
|---|---|---|
| 1. Modelo | Classes de domínio viraram entidades JPA com `@Entity` e chave UUID (`EntidadeBase`) | pacote `model` |
| 2. Repositórios | `JsonDataRepository` substituído por um `JpaRepository` por entidade | pacote `repository` |
| 3. Configuração | URL do banco por variável de ambiente; H2 como padrão local | `application.properties`, `compose.yaml` |
| 4. Carga inicial | Os dados do projeto eram sintéticos; `DadosIniciais` recria a massa de exemplo quando o banco está vazio (desligável com `rotavital.dados-iniciais=false`) | `config/DadosIniciais.java` |
| 5. Esquema | `spring.jpa.hibernate.ddl-auto=update` cria e evolui as tabelas | primeiro deploy no Render |
| 6. Validação | Testes de integração com banco real (H2 em memória, modo PostgreSQL) e teste de concorrência | 76+ testes verdes no GitHub Actions |
| 7. Limpeza | `JsonDataRepository` removido; nenhum arquivo `.json` de dados no repositório | commit `323ec45` |

**Rollback da Migração 1:** reverter o commit de integração (`git revert f845a73`) e reimplantar. Como os dados eram
sintéticos, não havia dado a converter de volta. A janela de risco já passou e a migração está estável.

**Efeito na rede:** a ligação aplicação → dados trocou NFSv4/2049 por protocolo PostgreSQL sobre TCP/5432 com pool de
conexões (HikariCP). A sonda do painel mede essa ligação continuamente.

## 5. Metas de desempenho (critérios de aceite)

Usadas para aprovar cada fase e para colorir o painel (OK / Atenção / Crítico).

| Métrica | Onde é medida | Meta (OK) | Crítico se |
|---|---|---|---|
| Tempo de resposta HTTP no servidor, p95 | Painel `/rede`, ligação Cliente → Aplicação | ≤ 300 ms | > 800 ms |
| Ida e volta aplicação → banco, p95 | Painel `/rede`, sonda `SELECT 1` | ≤ 20 ms | > 100 ms |
| Perda (timeout, erro ou HTTP 5xx) | Painel e benchmark | ≤ 1% | > 5% |
| Latência HTTPS de ponta a ponta, p95 | Benchmark externo (`benchmark_rede.py`) | ≤ 800 ms | > 2.000 ms |
| Vazão com 10 clientes simultâneos | Benchmark externo | ≥ 10 req/s | < 5 req/s |
| Jitter HTTPS | Benchmark externo | ≤ 50 ms | > 200 ms |

Por que esses números: a operação do banco de sangue é interativa (atendente, gestor, hospital consultando estoque),
então 300 ms no servidor e menos de 1 s de ponta a ponta mantêm a sensação de resposta imediata. O banco fica na
mesma rede da aplicação, então a ida e volta deve ser de poucos milissegundos; acima de 20 ms indica banco
sobrecarregado ou em outra região. O cold start do plano gratuito é registrado à parte e não entra nessas metas.

## 6. Migração 2 (planejada) — Render → rede própria (VPC)

Estratégia: **implantação paralela (blue/green)**. O ambiente novo sobe ao lado do atual, é medido com as mesmas
ferramentas e só recebe o tráfego quando cumpre as metas. O Render continua no ar como volta rápida.

| Fase | O que fazer | Como saber que deu certo | Responsável |
|---|---|---|---|
| **F0. Linha de base** | Rodar `benchmark_rede.py` contra o Render e guardar o resultado em `docs/rsd/benchmark/resultados/` | Relatório gerado e anexado ao [benchmarking](benchmarking.md) | Kelly, Elisa |
| **F1. Rede** | Criar a VPC 10.0.0.0/16, as três sub-redes e os grupos de segurança exatamente como na seção 3 | Teste de porta: 5432 inacessível da internet e da sub-rede pública; 8080 inacessível da internet | Elisa |
| **F2. Banco** | Criar PostgreSQL 15 na `sub-dados`; copiar os dados do Render com `pg_dump --format=custom` e `pg_restore`; conferir contagem de linhas das 8 tabelas | Contagens iguais nas duas bases; sonda do painel com p95 ≤ 20 ms | Mateus, Elisa |
| **F3. Aplicação** | Publicar a mesma imagem Docker na `sub-app` com as variáveis `SPRING_DATASOURCE_*` apontando para o banco novo e `rotavital.dados-iniciais=false` | `GET /api/v1/estoque` responde 200 de dentro da VPC; painel sem perda | Igor (SO), Mateus |
| **F4. Borda** | Subir o Nginx na `sub-publica`: só porta 443, certificado TLS (Let's Encrypt via desafio DNS-01, para não precisar abrir a 80), `proxy_pass` para a aplicação na 8080, cabeçalhos `X-Forwarded-*` | `curl -I https://<novo-endereço>` com TLS 1.3; nenhuma resposta na porta 80 | Kelly |
| **F5. Validação** | Rodar o benchmark contra o endereço novo e comparar com a linha de base (F0) | Todas as metas da seção 5 cumpridas; nenhuma métrica pior que a linha de base sem justificativa | Kelly, Elisa |
| **F6. Corte** | Reduzir o TTL do DNS para 60 s um dia antes; congelar escritas por ~10 min; repetir o `pg_dump` incremental (dados do dia); apontar o DNS para o Nginx | Painel do ambiente novo recebendo o tráfego; painel do Render zerado | Todos |
| **F7. Pós-corte** | Monitorar o painel por 48 h; manter o Render parado (não apagado) por 7 dias | Sem alerta crítico em 48 h; depois disso, desligar o Render | Kelly, Elisa |

### 6.1 Configuração mínima do proxy (F4)

```nginx
server {
    listen 443 ssl http2;
    server_name hemostart.exemplo.com.br;
    ssl_protocols TLSv1.3 TLSv1.2;
    ssl_certificate     /etc/letsencrypt/live/hemostart/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/hemostart/privkey.pem;

    location / {
        proxy_pass http://10.0.10.10:8080;          # aplicação na sub-app
        proxy_set_header Host              $host;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_connect_timeout 2s;
        proxy_read_timeout    30s;
    }
}
```

Na aplicação, `server.forward-headers-strategy=framework` faz o Spring entender que a requisição original era HTTPS.

## 7. Plano de volta (rollback)

| Se falhar em… | Sintoma | Ação | Tempo para voltar |
|---|---|---|---|
| F1–F5 | Meta não cumprida ou erro de configuração | Nada muda para o usuário (tráfego ainda no Render); corrigir e repetir a fase | 0 |
| F6 (corte) | Perda > 5% ou p95 HTTPS > 2 s no painel/benchmark | Voltar o DNS para o Render (TTL de 60 s); dados escritos no novo banco durante o corte são reaplicados no Render com `pg_dump --data-only` das tabelas alteradas | ~5 min + propagação do DNS |
| F7 (pós-corte) | Erro descoberto depois | Mesmo procedimento da F6, enquanto o Render existir (7 dias) | ~15 min |

**Gatilho objetivo:** qualquer métrica em **Crítico** por mais de 10 minutos seguidos depois do corte dispara o rollback.

## 8. Riscos

| Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|
| Regra de firewall errada expõe o banco | Média | Alto | Teste de porta na F1 antes de qualquer dado; banco sem IP público |
| Certificado TLS expira | Baixa | Alto | Renovação automática do Let's Encrypt e alerta 15 dias antes |
| Perda de dados no corte | Baixa | Alto | Escritas congeladas na janela; `pg_dump` final; contagem de linhas antes/depois |
| Latência maior que no Render (região diferente) | Média | Médio | Escolher região mais próxima dos usuários (São Paulo); comparar com a linha de base na F5 |
| Concorrência na troca de banco (reserva duplicada) | Baixa | Alto | Seção crítica (`SecaoCritica`) já serializa reservas e o commit; teste de concorrência no CI |
| Custo acima do orçamento | Média | Médio | Usar o menor tamanho de máquina que cumpra as metas; planilha de orçamento de SO (ADR 0001) |

## 9. Checklist de corte (F6)

- [ ] Linha de base (F0) e validação (F5) anexadas ao [benchmarking](benchmarking.md)
- [ ] Todas as metas da seção 5 cumpridas no ambiente novo
- [ ] TTL do DNS em 60 s há pelo menos 24 h
- [ ] Equipe avisada da janela de manutenção (Slack)
- [ ] Escritas congeladas → `pg_dump` final → `pg_restore` → contagens conferidas
- [ ] DNS apontado para o Nginx
- [ ] Painel `/rede` do ambiente novo recebendo tráfego, sem alerta crítico
- [ ] Render parado, mas não apagado, por 7 dias

## 10. Cronograma proposto

| Quando | Fases |
|---|---|
| Semana de 23/11 | F0 (linha de base) e F1 (rede) |
| Semana de 30/11 | F2 a F5 |
| Depois da apresentação final | F6 e F7, se o grupo optar por manter o sistema no ar |

Até a apresentação, o ambiente de produção continua sendo o Render; a Migração 2 fica documentada e validável pelas
mesmas ferramentas (benchmark e painel).
