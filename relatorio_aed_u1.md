# Relatório AED Unidade 1

**Título:** Entrega U1 - Projeto HemoStart  
**Aluno:** Augusto Freitas Wanderley

## 1. Introdução
O presente relatório descreve a implementação das estruturas de dados básicas do sistema HemoStart, referentes à Unidade 1, na linguagem C++. Foram desenvolvidas três estruturas de domínio: Lista Encadeada (Estoque), Fila (Requisições) e Pilha (Histórico). Todas as estruturas foram construídas utilizando alocação manual de memória e manipulação explícita de ponteiros, conforme os requisitos da disciplina.

## 2. Estrutura de Lista (Estoque)
A estrutura de estoque foi implementada no arquivo `ListaEstoque.cpp` utilizando uma Lista Encadeada. A estrutura base é um `struct Bolsa`, que contém os dados da bolsa de sangue e um ponteiro explícito para o próximo nó (`Bolsa* proximo`). A inserção de novos elementos é feita no início da lista, utilizando alocação dinâmica de memória com o operador `new`. Para evitar vazamentos de memória (memory leaks), a classe possui um destrutor (`~ListaEstoque`) que percorre toda a lista e libera a memória de cada nó manualmente utilizando o operador `delete`. A operação de inserção possui complexidade O(1).

## 3. Estrutura de Fila (Requisições)
A fila de requisições foi implementada no arquivo `FilaRequisicoes.cpp` seguindo a política FIFO (First In, First Out). A estrutura base é um `struct Requisicao`, e o controle da fila é feito através de dois ponteiros explícitos: `frente` e `tras`. A operação de enfileirar (`enfileirar`) aloca memória dinamicamente com `new` e atualiza o ponteiro `tras`. A operação de desenfileirar (`desenfileirar`) atualiza o ponteiro `frente` e libera a memória do nó removido com `delete`, garantindo que a memória seja gerenciada corretamente pelo programador. Ambas as operações possuem complexidade O(1).

## 4. Estrutura de Pilha (Histórico)
A pilha de histórico foi implementada no arquivo `PilhaHistorico.cpp` seguindo a política LIFO (Last In, First Out). A estrutura base é um `struct AcaoHistorico`, controlada por um único ponteiro explícito chamado `topo`. A operação de empilhar (`push`) aloca memória dinamicamente com `new` e insere o novo nó no topo. A operação de desempilhar (`pop`) atualiza o ponteiro `topo` e libera a memória do nó removido com `delete`. O destrutor da classe percorre a pilha liberando toda a memória alocada, prevenindo vazamentos.

## 5. Conclusão
A Unidade 1 foi concluída com êxito. As estruturas de Lista, Fila e Pilha foram implementadas em C++ com alocação manual de memória e uso explícito de ponteiros, demonstrando o domínio dos conceitos fundamentais de gerenciamento de memória exigidos no escopo do projeto.
