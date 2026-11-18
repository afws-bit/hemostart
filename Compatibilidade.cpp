#ifndef COMPATIBILIDADE_CPP
#define COMPATIBILIDADE_CPP

#include <string>

// =====================================================================
// AED - Unidade 2: Compatibilidade ABO/Rh via MATRIZ
// Linha = tipo do DOADOR (bolsa), coluna = tipo do RECEPTOR (paciente).
// MATRIZ[d][r] == true  ->  a bolsa do tipo d pode ser transfundida em r.
// Consulta: O(1) apos converter o rotulo em indice (busca sequencial em 8 posicoes).
// =====================================================================

const int QTD_TIPOS = 8;

// Mesma ordem usada na versao Java (MatrizCompatibilidade.TIPOS)
const std::string TIPOS[QTD_TIPOS] = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

//                              receptor:  A+     A-     B+     B-     AB+    AB-    O+     O-
const bool MATRIZ[QTD_TIPOS][QTD_TIPOS] = {
    /* doador A+  */ {true,  false, false, false, true,  false, false, false},
    /* doador A-  */ {true,  true,  false, false, true,  true,  false, false},
    /* doador B+  */ {false, false, true,  false, true,  false, false, false},
    /* doador B-  */ {false, false, true,  true,  true,  true,  false, false},
    /* doador AB+ */ {false, false, false, false, true,  false, false, false},
    /* doador AB- */ {false, false, false, false, true,  true,  false, false},
    /* doador O+  */ {true,  false, true,  false, true,  false, true,  false},
    /* doador O-  */ {true,  true,  true,  true,  true,  true,  true,  true }
};

// Busca sequencial do rotulo no vetor TIPOS. Retorna -1 se invalido. O(8) = O(1).
int indiceTipo(const std::string& tipo) {
    for (int i = 0; i < QTD_TIPOS; i++) {
        if (TIPOS[i] == tipo) return i;
    }
    return -1;
}

bool compativel(const std::string& tipoDoador, const std::string& tipoReceptor) {
    int d = indiceTipo(tipoDoador);
    int r = indiceTipo(tipoReceptor);
    if (d < 0 || r < 0) return false;
    return MATRIZ[d][r];
}

#endif
