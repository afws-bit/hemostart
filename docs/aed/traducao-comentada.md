# AED — Tradução comentada C++ ↔ Java (Unidades 1 e 2)

> Projeto Integrador ADS 2026.2 — Equipe Hemostart — Sistema **Rota Vital**
>
> Este documento acompanha as duas versões de cada estrutura e algoritmo:
> o código **C++** (raiz do repositório) e a **reimplementação em Java**, usada de verdade pela aplicação Spring Boot.
> Para cada uma: onde está, por que a versão Java corresponde à C++ (com base nas linhas reais) e, na U2, a complexidade.

## Como executar

| Versão | Comando | O que mostra |
|---|---|---|
| C++ | `g++ -std=c++17 -Wall main.cpp -o hemostart && ./hemostart` | Demonstração da U1 + **24 verificações automáticas** da U2 (sai com código 0 se todas passam) |
| Java | `mvn test` | `AedAlgoritmosTest` (mesmos casos do `main.cpp`) + testes de integração com o banco |
| Aplicação | `mvn spring-boot:run` → **Requisições → Reservar** | O pedido é atendido pelos 4 algoritmos e a rota aparece na coluna **Rota de entrega** |

Validação de memória da versão C++: `g++ -std=c++17 -fsanitize=address,undefined -g main.cpp && ./a.out` termina sem nenhum vazamento ou acesso inválido.

## Mapa de arquivos

| Estrutura / algoritmo | C++ | Java | Onde o Java é usado na aplicação |
|---|---|---|---|
| Lista encadeada (estoque) | `ListaEstoque.cpp` | `estruturas/ListaEncadeada.java` | `EstoqueService.listarEstoque()` |
| Fila (requisições) | `FilaRequisicoes.cpp` | `estruturas/FilaRequisicoes.java` | `RequisicaoService.listarFila()` |
| Pilha (histórico) | `PilhaHistorico.cpp` | `estruturas/PilhaHistorico.java` | `HistoricoService.listarHistorico()` |
| Compatibilidade ABO/Rh | `Compatibilidade.cpp` | `aed/MatrizCompatibilidade.java` | `CompatibilidadeService`, `TipoSanguineo` |
| FEFO | `FilaFEFO.cpp` | `aed/FilaFEFO.java` | dentro do índice por hash |
| Índice por hash | `TabelaHash.cpp` | `aed/TabelaHashEstoque.java` | `RequisicaoService.reservarBolsasParaPrimeiro()` |
| Roteirização | `Grafo.cpp` | `aed/GrafoRotas.java` + `aed/Rota.java` | idem; mapa em `config/MapaRotasConfig.java` |
| Integração (pedido → bolsa + rota) | `Alocador.cpp` | `aed/AlocadorBolsas.java` | idem |

### Diferença de memória (vale para todas as estruturas)

No C++ cada nó é criado com `new` e destruído com `delete`, explicitamente, nos métodos de remoção e no destrutor (`~ListaEstoque`, `~FilaRequisicoes`, `~PilhaHistorico`, `~FilaFEFO`, `~TabelaHashEstoque`). Em Java não existe `delete`: o equivalente é **deixar o nó sem nenhuma referência** (`inicio = inicio.proximo;`), e o coletor de lixo libera a memória. Ponteiros `Tipo*` viram referências; `nullptr` vira `null`; `ponteiro->campo` vira `referencia.campo`. A lógica de ligar e desligar nós é a mesma, linha a linha.

---

# Unidade 1 — Estruturas básicas

## Lista encadeada — estoque

**C++ (`ListaEstoque.cpp`)**
```cpp
Bolsa* nova = new Bolsa();
nova->proximo = inicio;
inicio = nova;
```
**Java (`ListaEncadeada.java`, `inserirInicio`)**
```java
No<T> novo = new No<>(dado);
novo.next = head;
head = novo;
```
**Por que corresponde:** as duas versões guardam um único ponteiro para o primeiro nó (`inicio` / `head`) e inserem no início com as mesmas três operações: criar o nó, apontar o `proximo` dele para o antigo início e mover o início para o novo nó, em O(1). A listagem também é igual: um laço `while (atual != nullptr)` / `while (atual != null)` que avança por `proximo`. A versão Java é **genérica** (`ListaEncadeada<T>`) porque a aplicação guarda objetos `BolsaSangue` completos em vez de `id/tipo/quantidade`. Ela também traz `inserirOrdenado`, `remover` e `buscar`, que percorrem a mesma cadeia de nós, para a aplicação montar o estoque ordenado por validade.

## Fila — requisições hospitalares

**C++ (`FilaRequisicoes.cpp`)**
```cpp
if (tras == nullptr) { frente = tras = nova; }
else { tras->proximo = nova; tras = nova; }
...
Requisicao* temp = frente; frente = frente->proximo;
if (frente == nullptr) tras = nullptr;
delete temp;
```
**Java (`FilaRequisicoes.java`, `enfileirar` para pedido normal e `desenfileirar`)**
```java
if (tail == null) { head = novo; tail = novo; }
else { tail.next = novo; tail = novo; }
...
Requisicao dado = head.dado; head = head.next;
if (head == null) tail = null;
```
**Por que corresponde:** as duas filas mantêm **dois ponteiros** (`frente/tras` = `head/tail`) para que tanto enfileirar quanto desenfileirar sejam O(1). O caso especial "fila vazia → frente e trás apontam para o mesmo nó" e o caso "removi o último → trás volta a nulo" aparecem nas duas. A única extensão do Java é de regra de negócio (HU05): um pedido **urgente** é inserido logo depois do último urgente, à frente dos normais. Pedidos normais seguem exatamente o FIFO do C++.

## Pilha — histórico de operações

**C++ (`PilhaHistorico.cpp`)**
```cpp
nova->proximo = topo; topo = nova;          // push
AcaoHistorico* temp = topo; topo = topo->proximo; delete temp;   // pop
```
**Java (`PilhaHistorico.java`)**
```java
novo.next = top; top = novo;                // empilhar
T dado = top.dado; top = top.next;          // desempilhar
```
**Por que corresponde:** um único ponteiro `topo`/`top`; empilhar faz o novo nó apontar para o topo antigo e vira o topo; desempilhar move o topo para o próximo. LIFO, O(1) nas duas. Na aplicação, `HistoricoService.listarHistorico()` lê as operações do banco em ordem cronológica e as **empilha**. Ao percorrer a pilha do topo, o histórico aparece do mais recente para o mais antigo, que é justamente o uso de uma pilha.

---

# Unidade 2 — Algoritmos de inteligência

## 1. Compatibilidade ABO/Rh — matriz

**C++ (`Compatibilidade.cpp`)**
```cpp
const bool MATRIZ[QTD_TIPOS][QTD_TIPOS] = { /* 8 x 8 */ };
int d = indiceTipo(tipoDoador); int r = indiceTipo(tipoReceptor);
if (d < 0 || r < 0) return false;
return MATRIZ[d][r];
```
**Java (`MatrizCompatibilidade.java`)**
```java
private static final boolean[][] MATRIZ = { /* 8 x 8 */ };
int d = indiceTipo(tipoDoador); int r = indiceTipo(tipoReceptor);
if (d < 0 || r < 0) return false;
return MATRIZ[d][r];
```
**Por que corresponde:** o vetor `TIPOS` tem a **mesma ordem** nas duas versões (A+, A-, B+, B-, AB+, AB-, O+, O-), e a matriz foi copiada célula a célula. A linha é o doador (bolsa) e a coluna é o receptor (paciente). `indiceTipo` é a mesma busca sequencial nas duas, com retorno -1 para rótulo inválido. Por isso `compativel("X", "A+")` é falso em ambas. Na aplicação existe uma única fonte da regra: `TipoSanguineo.podeDoarPara()` e `CompatibilidadeService.compativel()` consultam esta matriz.

**Complexidade:** `indiceTipo` faz no máximo 8 comparações e a consulta é 1 acesso a `MATRIZ[d][r]`. Como 8 é fixo, o custo é **O(1)**. Espaço: 64 booleanos, O(1).

**Testes de compatível × incompatível:** `main.cpp`, bloco 1 (9 verificações), e `AedAlgoritmosTest.matriz_casosCompativeis`, `matriz_casosIncompativeis` e `matriz_quantidadeDeReceptoresPorDoador` (confere que cada linha tem 2, 4, 2, 4, 1, 2, 4 e 8 receptores). O `RotaVitalTests.compatibilidade_tabelaCompletaABORh` compara as 64 células com a tabela oficial.

## 2. Priorização FEFO — inserção ordenada

**C++ (`FilaFEFO.cpp`, `inserir`)**
```cpp
if (inicio == nullptr || validade < inicio->dataValidade) { nova->proximo = inicio; inicio = nova; return; }
BolsaFEFO* atual = inicio;
while (atual->proximo != nullptr && atual->proximo->dataValidade <= validade) atual = atual->proximo;
nova->proximo = atual->proximo; atual->proximo = nova;
```
**Java (`aed/FilaFEFO.java`, `inserir`)**
```java
if (inicio == null || bolsa.getDataValidade().isBefore(inicio.bolsa.getDataValidade())) { nova.proximo = inicio; inicio = nova; return; }
No atual = inicio;
while (atual.proximo != null && !atual.proximo.bolsa.getDataValidade().isAfter(bolsa.getDataValidade())) atual = atual.proximo;
nova.proximo = atual.proximo; atual.proximo = nova;
```
**Por que corresponde:** as mesmas duas situações aparecem nas duas versões. Se a nova bolsa vence antes de todas, ela vira o início. Senão, o laço anda até o último nó com validade **menor ou igual** e encaixa a nova depois dele. O `<=` do C++ e o `!isAfter` do Java são a mesma condição, e é ela que mantém a **ordem de chegada em caso de empate** (teste `fefo_empateMantemOrdemDeChegada`). Muda só a representação da data: em C++ é o inteiro `AAAAMMDD` (20261120 < 20261215 preserva a ordem cronológica) e em Java é `LocalDate`, comparado com `isBefore`/`isAfter`. A retirada remove o primeiro nó nas duas.

**Complexidade:**
- **Inserir: O(n)** no pior caso, quando a bolsa vence por último e o laço percorre os n nós. O melhor caso é O(1), quando ela entra no início.
- **Consultar ou retirar a mais urgente: O(1)**, porque a ordenação já foi paga na inserção e a próxima a vencer está sempre em `inicio`.

Esse custo compensa: cada bolsa é inserida uma vez e consultada muitas vezes.

## 3. Índice de estoque por hash

**C++ (`TabelaHash.cpp`)**
```cpp
unsigned long hash = 5381;
for (char c : chave) hash = ((hash << 5) + hash) + (unsigned char) c;   // hash*33 + c
return (int) (hash % TAMANHO_TABELA);                                   // 101
```
**Java (`TabelaHashEstoque.java`)**
```java
long hash = 5381;
for (int i = 0; i < chave.length(); i++) hash = ((hash << 5) + hash) + (chave.charAt(i) & 0xFF);
return (int) Long.remainderUnsigned(hash, TAMANHO_TABELA);              // 101
```
**Por que corresponde:**
- **Mesma função djb2:** começa em 5381, multiplica por 33 (`<< 5` mais o próprio valor) e soma o caractere.
- **Mesmo módulo:** 101, um número primo que espalha melhor as chaves.
- **Sem sinal nas duas versões:** o C++ usa `unsigned long` (64 bits). O `long` do Java tem sinal, então o resto é calculado com `Long.remainderUnsigned`, e o `& 0xFF` reproduz o `(unsigned char)`. O estouro de 64 bits dá o mesmo padrão de bits nas duas linguagens, e por isso os índices são idênticos: `hash("O-") = 16` e `hash("A+") = 57`. Isso é impresso pelo `main.cpp` e conferido em `hash_mesmosIndicesDaVersaoCpp`.

As colisões são tratadas por **encadeamento separado** nas duas versões. Cada balde guarda uma lista ligada de `EntradaHash` (chave, fila FEFO, `proximo`), e a nova entrada vai para o início do balde. A chave é o tipo sanguíneo e o valor é a `FilaFEFO` daquele tipo, de modo que o índice já devolve as bolsas em ordem de validade.

**Complexidade:**
- **Calcular o hash:** O(|chave|) ≤ 3 caracteres, ou seja, O(1).
- **Buscar e inserir:** O(1) em média, porque há no máximo 8 chaves em 101 baldes e a chance de colisão é baixa. No pior caso é O(k), com k entradas no mesmo balde, ou seja, k ≤ 8.
- **Inserir uma bolsa:** soma O(n) da inserção ordenada na FEFO do tipo.
- **Espaço:** O(101 + n).

## 4. Roteirização — vizinho mais próximo

**C++ (`Grafo.cpp`, `rotaVizinhoMaisProximo`)**
```cpp
if (distancia[atual][alvo] > 0) proximo = alvo;
else for (int j = 0; j < qtdLocais; j++)
    if (distancia[atual][j] > 0 && !visitado[j] && (proximo == -1 || distancia[atual][j] < menor)) { proximo = j; menor = distancia[atual][j]; }
if (proximo == -1) return rota;   // beco sem saída
```
**Java (`GrafoRotas.java`)**
```java
if (distancia[atual][alvo] > 0) proximo = alvo;
else for (int j = 0; j < qtdLocais; j++)
    if (distancia[atual][j] > 0 && !visitado[j] && (proximo == -1 || distancia[atual][j] < menor)) { proximo = j; menor = distancia[atual][j]; }
if (proximo == -1) return Rota.naoEncontrada();
```
**Por que corresponde:**
- **Mesmo grafo:** não direcionado, guardado numa matriz de adjacência `int distancia[20][20]`, em que 0 significa "sem estrada" e cada rota é gravada nas duas direções.
- **Mesma regra gulosa:** se o destino é vizinho direto, vai a ele. Senão, escolhe o vizinho **não visitado** mais próximo (vetor `visitado`), o que impede andar em círculos.
- **Mesmo resultado:** os nomes dos locais ficam num vetor paralelo, a rota é a lista de paradas e a distância é acumulada. As duas devolvem "Hemocentro → Centro → Hospital Municipal Norte (11 km)" para o mesmo mapa.
- **Na aplicação:** o mapa é montado em `MapaRotasConfig`.

**Complexidade:** a cada passo um vértice novo é visitado, então há no máximo V passos, e cada passo varre uma linha da matriz com V colunas, o que dá **O(V²)**. A matriz ocupa O(V²) de espaço.

É uma **heurística**: decide pelo vizinho mais próximo agora e não garante o menor caminho total, que exigiria Dijkstra, com O((V+E) log V). Para o mapa pequeno do hemocentro ela é suficiente e simples de explicar. Se o caminho guloso bater num beco sem saída, a rota é marcada como não encontrada e a reserva continua, com "Rota não mapeada".

## 5. Integração — pedido → bolsa compatível + validade + rota

**C++ (`Alocador.cpp`) ↔ Java (`AlocadorBolsas.java`).** As duas classes seguem a mesma ordem de passos:

1. **Confere o estoque:** soma `estoque.quantidade(tipo)` de todos os tipos com `compativel(tipo, paciente)` (matriz + hash). Se não houver bolsas suficientes, **nada é retirado** e o pedido não é atendido.
2. **Mesmo tipo primeiro:** esgota a fila FEFO do **mesmo tipo** do paciente (hash → FEFO), para não gastar O- universal à toa.
3. **Outros compatíveis:** para cada bolsa que ainda falta, compara a **frente** da fila FEFO de cada tipo compatível e retira a que **vence primeiro**. É o FEFO aplicado entre os tipos.
4. **Rota:** calcula `rotaVizinhoMaisProximo(Hemocentro, hospital)`.

**Complexidade de um pedido de q bolsas:**
- passo 1: O(8);
- passo 2: O(q);
- passo 3: O(q · 8), porque cada bolsa compara no máximo 8 frentes de fila, e cada busca no hash é O(1) em média;
- rota: O(V²);
- **total: O(q + V²)**, já que 8 é constante.

Na aplicação, montar o índice custa mais O(n·m): as n bolsas disponíveis são lidas do banco e inseridas na FEFO de seu tipo, com m bolsas por tipo.

**Na aplicação Spring:** `RequisicaoService.reservarBolsasParaPrimeiro()` atende o pedido com os quatro algoritmos:
1. lê as bolsas disponíveis do banco;
2. monta a `TabelaHashEstoque`;
3. chama o `AlocadorBolsas`;
4. reserva as bolsas escolhidas;
5. grava a rota e os km na requisição, que aparecem em **Requisições → Rota de entrega**.

Teste de ponta a ponta com banco: `HistoriasIntegracaoTest.aedU2_reservaCalculaRotaParaHospitalDoMapa`.

### Casos verificados nas duas versões

| Caso | `main.cpp` | `AedAlgoritmosTest` |
|---|---|---|
| A+ ×2 → A+ id 3 e depois A- id 4 (vence antes do O-), rota de 11 km | bloco 5 | `integracao_pedidoCompletoComRota` |
| O- ×3 com só 2 O- → recusado, estoque intacto | bloco 5 | idem |
| B- ×1 → recebe O-, nunca B+ (incompatível) | bloco 5 | idem |
| AB- ×2 com só 1 compatível → recusado | bloco 5 | idem |
| FEFO: 202 (20/11) antes de 203 e 201 | bloco 2 | `fefo_insercaoOrdenadaERetirada` |
| hash("O-") = 16, hash("A+") = 57 | bloco 3 | `hash_mesmosIndicesDaVersaoCpp` |
| Hemocentro → Centro → Municipal Norte = 11 km | bloco 4 | `rota_vizinhoMaisProximo` |

## Resumo de complexidades

| Operação | Tempo | Espaço |
|---|---|---|
| Lista: inserir no início / listar | O(1) / O(n) | O(n) |
| Fila: enfileirar / desenfileirar | O(1) / O(1) | O(n) |
| Pilha: empilhar / desempilhar | O(1) / O(1) | O(n) |
| Compatibilidade (matriz) | O(1) | O(1) — 64 células |
| FEFO: inserir / retirar a mais urgente | O(n) / O(1) | O(n) |
| Hash: buscar / inserir chave | O(1) médio, O(k) pior | O(101 + n) |
| Rota (vizinho mais próximo) | O(V²) | O(V²) |
| Alocar pedido de q bolsas | O(q + V²) | O(q) |
