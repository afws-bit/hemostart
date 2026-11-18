package br.com.rotavital.aed;

/**
 * AED - Unidade 2: compatibilidade ABO/Rh via MATRIZ.
 * Tradução direta de Compatibilidade.cpp: mesmo vetor TIPOS, mesma matriz 8x8
 * (linha = doador/bolsa, coluna = receptor/paciente) e mesma busca sequencial do índice.
 * Consulta O(1): no máximo 8 comparações para achar cada índice + 1 acesso à matriz.
 */
public final class MatrizCompatibilidade {

    public static final int QTD_TIPOS = 8;

    /** Mesma ordem de Compatibilidade.cpp. */
    public static final String[] TIPOS = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    //                                       receptor:  A+     A-     B+     B-     AB+    AB-    O+     O-
    private static final boolean[][] MATRIZ = {
            /* doador A+  */ {true,  false, false, false, true,  false, false, false},
            /* doador A-  */ {true,  true,  false, false, true,  true,  false, false},
            /* doador B+  */ {false, false, true,  false, true,  false, false, false},
            /* doador B-  */ {false, false, true,  true,  true,  true,  false, false},
            /* doador AB+ */ {false, false, false, false, true,  false, false, false},
            /* doador AB- */ {false, false, false, false, true,  true,  false, false},
            /* doador O+  */ {true,  false, true,  false, true,  false, true,  false},
            /* doador O-  */ {true,  true,  true,  true,  true,  true,  true,  true }
    };

    private MatrizCompatibilidade() {}

    /** Busca sequencial do rótulo. Retorna -1 se inválido. */
    public static int indiceTipo(String tipo) {
        for (int i = 0; i < QTD_TIPOS; i++) {
            if (TIPOS[i].equals(tipo)) return i;
        }
        return -1;
    }

    public static boolean compativel(String tipoDoador, String tipoReceptor) {
        int d = indiceTipo(tipoDoador);
        int r = indiceTipo(tipoReceptor);
        if (d < 0 || r < 0) return false;
        return MATRIZ[d][r];
    }
}
