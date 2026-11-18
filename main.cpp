#include <iostream>
#include "ListaEstoque.cpp"
#include "FilaRequisicoes.cpp"
#include "PilhaHistorico.cpp"
#include "Compatibilidade.cpp"
#include "FilaFEFO.cpp"
#include "TabelaHash.cpp"
#include "Grafo.cpp"
#include "Alocador.cpp"

// Contadores dos testes automaticos
int testesOk = 0, testesFalha = 0;
void verificar(bool condicao, const std::string& descricao) {
    if (condicao) { testesOk++;    std::cout << "  [OK]    " << descricao << "\n"; }
    else          { testesFalha++; std::cout << "  [FALHA] " << descricao << "\n"; }
}

int main() {
    std::cout << "=== Sistema HemoStart / Rota Vital - AED (C++) ===\n\n";

    // ------------------------- UNIDADE 1 -------------------------
    std::cout << "##### UNIDADE 1 #####\n";
    ListaEstoque estoqueLista;
    estoqueLista.inserir(1, "O-", 10);
    estoqueLista.inserir(2, "A+", 5);
    estoqueLista.listar();

    FilaRequisicoes fila;
    fila.enfileirar(101, "Hospital Central");
    fila.enfileirar(102, "Hospital Sul");
    std::cout << "\nRequisicoes enfileiradas com sucesso.\n";

    PilhaHistorico historico;
    historico.push("Doacao registrada: O-");
    historico.push("Requisicao criada: Hospital Central");
    historico.imprimir();

    // ------------------------- UNIDADE 2 -------------------------
    std::cout << "\n##### UNIDADE 2 #####\n";

    std::cout << "\n--- 1) Compatibilidade ABO/Rh (matriz) ---\n";
    verificar(compativel("O-", "AB+"),  "O- doa para AB+ (doador universal)");
    verificar(compativel("O-", "O-"),   "O- doa para O-");
    verificar(compativel("A-", "A+"),   "A- doa para A+");
    verificar(compativel("AB+", "AB+"), "AB+ doa para AB+");
    verificar(!compativel("A+", "O-"),  "A+ NAO doa para O-");
    verificar(!compativel("A+", "B-"),  "A+ NAO doa para B-");
    verificar(!compativel("O+", "O-"),  "O+ NAO doa para O- (Rh)");
    verificar(!compativel("AB+", "A+"), "AB+ NAO doa para A+");
    verificar(!compativel("X", "A+"),   "tipo invalido e rejeitado");

    std::cout << "\n--- 2) FEFO (insercao ordenada) ---\n";
    FilaFEFO fefo;
    fefo.inserir(201, "O+", 4, 20261215);
    fefo.inserir(202, "O+", 2, 20261120); // vence primeiro
    fefo.inserir(203, "A-", 6, 20261130);
    fefo.listar();
    verificar(fefo.proximaAVencer()->id == 202, "frente da fila e a bolsa que vence primeiro (202)");
    fefo.retirarProximaAVencer();
    verificar(fefo.proximaAVencer()->id == 203, "apos retirar, a proxima e a 203");

    std::cout << "\n--- 3) Indice de estoque por hash ---\n";
    TabelaHashEstoque estoque;
    estoque.inserir(1, "O-", 1, 20261210);
    estoque.inserir(2, "O-", 1, 20261125); // O- que vence antes
    estoque.inserir(3, "A+", 1, 20261201);
    estoque.inserir(4, "A-", 1, 20261122);
    estoque.inserir(5, "B+", 1, 20261205);
    std::cout << "hash(\"O-\") = " << estoque.funcaoHash("O-") << " | hash(\"A+\") = " << estoque.funcaoHash("A+") << "\n";
    verificar(estoque.quantidade("O-") == 2, "dois O- no indice");
    verificar(estoque.buscar("AB-") == nullptr, "tipo sem bolsas retorna nullptr");
    verificar(estoque.buscar("O-")->proximaAVencer()->id == 2, "dentro do tipo, FEFO: O- id 2 na frente");

    std::cout << "\n--- 4) Roteirizacao (vizinho mais proximo) ---\n";
    GrafoHospitalar mapa;
    mapa.adicionarRota("Hemocentro", "Centro", 4);
    mapa.adicionarRota("Hemocentro", "Hospital Santa Cruz", 12);
    mapa.adicionarRota("Centro", "Hospital Municipal Norte", 7);
    mapa.adicionarRota("Centro", "Zona Sul", 5);
    mapa.adicionarRota("Zona Sul", "Hospital Santa Cruz", 6);
    mapa.exibirRotas();
    Rota r1 = mapa.rotaVizinhoMaisProximo("Hemocentro", "Hospital Municipal Norte");
    imprimirRota(r1);
    verificar(r1.encontrada && r1.distanciaTotal == 11, "Hemocentro -> Centro -> Municipal Norte = 11 km");
    Rota r2 = mapa.rotaVizinhoMaisProximo("Hemocentro", "Hospital Santa Cruz");
    imprimirRota(r2);
    verificar(r2.encontrada && r2.qtdParadas == 2, "destino vizinho direto: vai direto (12 km)");
    verificar(!mapa.rotaVizinhoMaisProximo("Hemocentro", "Inexistente").encontrada, "local desconhecido: rota nao encontrada");

    std::cout << "\n--- 5) Integracao: pedido -> bolsa compativel + FEFO + rota ---\n";
    Alocador alocador(estoque, mapa, "Hemocentro");

    Pedido p1 = {"Hospital Municipal Norte", "A+", 2};
    ResultadoAlocacao a1 = alocador.alocar(p1);
    for (int i = 0; i < a1.qtdBolsas; i++)
        std::cout << "  bolsa " << a1.bolsas[i].id << " (" << a1.bolsas[i].tipoSanguineo
                  << ", vence " << a1.bolsas[i].dataValidade << ")\n";
    std::cout << "  rota: "; imprimirRota(a1.rota);
    verificar(a1.atendido && a1.qtdBolsas == 2, "pedido A+ x2 atendido");
    verificar(a1.bolsas[0].id == 3, "1a bolsa: mesmo tipo do paciente (A+ id 3)");
    verificar(a1.bolsas[1].id == 4, "2a bolsa: compativel que vence primeiro (A- id 4, antes do O-)");

    Pedido p2 = {"Hospital Santa Cruz", "O-", 3};
    ResultadoAlocacao a2 = alocador.alocar(p2);
    verificar(!a2.atendido && estoque.quantidade("O-") == 2, "O- x3 sem estoque suficiente: nada e retirado");

    Pedido p3 = {"Hospital Santa Cruz", "B-", 1};
    ResultadoAlocacao a3 = alocador.alocar(p3);
    verificar(a3.atendido && a3.bolsas[0].tipoSanguineo == "O-",
              "B- recebe O- (compativel), nunca o B+ id 5 (incompativel)");
    verificar(estoque.quantidade("B+") == 1, "B+ continua no estoque");

    Pedido p4 = {"Hospital Santa Cruz", "AB-", 2};
    verificar(!alocador.alocar(p4).atendido, "AB- x2 com so 1 compativel (O-) - pedido recusado");

    std::cout << "\nResultado: " << testesOk << " testes OK, " << testesFalha << " falhas\n";
    return testesFalha == 0 ? 0 : 1;
}
