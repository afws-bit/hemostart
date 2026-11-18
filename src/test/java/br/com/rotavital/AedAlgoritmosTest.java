package br.com.rotavital;

import br.com.rotavital.aed.*;
import br.com.rotavital.model.BolsaSangue;
import br.com.rotavital.model.StatusBolsa;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários da AED U2 (sem Spring). Reproduzem, um a um, os casos
 * verificados pelo main.cpp da versão C++, para provar que as duas versões
 * produzem o mesmo resultado.
 */
class AedAlgoritmosTest {

    private static BolsaSangue bolsa(String codigo, String tipo, LocalDate validade) {
        BolsaSangue b = new BolsaSangue();
        b.setCodigo(codigo);
        b.setTipoSanguineo(tipo);
        b.setDataValidade(validade);
        b.setStatus(StatusBolsa.DISPONIVEL);
        return b;
    }

    private static LocalDate d(int aaaammdd) {
        return LocalDate.of(aaaammdd / 10000, aaaammdd / 100 % 100, aaaammdd % 100);
    }

    private static GrafoRotas mapa() {
        GrafoRotas m = new GrafoRotas();
        m.adicionarRota("Hemocentro", "Centro", 4);
        m.adicionarRota("Hemocentro", "Hospital Santa Cruz", 12);
        m.adicionarRota("Centro", "Hospital Municipal Norte", 7);
        m.adicionarRota("Centro", "Zona Sul", 5);
        m.adicionarRota("Zona Sul", "Hospital Santa Cruz", 6);
        return m;
    }

    private static TabelaHashEstoque estoque() {
        TabelaHashEstoque e = new TabelaHashEstoque();
        e.inserir(bolsa("1", "O-", d(20261210)));
        e.inserir(bolsa("2", "O-", d(20261125)));
        e.inserir(bolsa("3", "A+", d(20261201)));
        e.inserir(bolsa("4", "A-", d(20261122)));
        e.inserir(bolsa("5", "B+", d(20261205)));
        return e;
    }

    // ---------- 1) Matriz de compatibilidade ----------

    @Test
    void matriz_casosCompativeis() {
        assertTrue(MatrizCompatibilidade.compativel("O-", "AB+"));
        assertTrue(MatrizCompatibilidade.compativel("O-", "O-"));
        assertTrue(MatrizCompatibilidade.compativel("A-", "A+"));
        assertTrue(MatrizCompatibilidade.compativel("AB+", "AB+"));
    }

    @Test
    void matriz_casosIncompativeis() {
        assertFalse(MatrizCompatibilidade.compativel("A+", "O-"));
        assertFalse(MatrizCompatibilidade.compativel("A+", "B-"));
        assertFalse(MatrizCompatibilidade.compativel("O+", "O-"));
        assertFalse(MatrizCompatibilidade.compativel("AB+", "A+"));
        assertFalse(MatrizCompatibilidade.compativel("X", "A+"));
    }

    @Test
    void matriz_quantidadeDeReceptoresPorDoador() {
        int[] esperado = {2, 4, 2, 4, 1, 2, 4, 8}; // A+ A- B+ B- AB+ AB- O+ O-
        for (int d = 0; d < 8; d++) {
            int total = 0;
            for (int r = 0; r < 8; r++) {
                if (MatrizCompatibilidade.compativel(MatrizCompatibilidade.TIPOS[d], MatrizCompatibilidade.TIPOS[r])) total++;
            }
            assertEquals(esperado[d], total, "receptores de " + MatrizCompatibilidade.TIPOS[d]);
        }
    }

    // ---------- 2) FEFO ----------

    @Test
    void fefo_insercaoOrdenadaERetirada() {
        FilaFEFO fefo = new FilaFEFO();
        fefo.inserir(bolsa("201", "O+", d(20261215)));
        fefo.inserir(bolsa("202", "O+", d(20261120)));
        fefo.inserir(bolsa("203", "A-", d(20261130)));
        assertEquals(List.of("202", "203", "201"), fefo.paraLista().stream().map(BolsaSangue::getCodigo).toList());
        assertEquals("202", fefo.retirarProximaAVencer().getCodigo());
        assertEquals("203", fefo.proximaAVencer().getCodigo());
        assertEquals(2, fefo.getTamanho());
    }

    @Test
    void fefo_empateMantemOrdemDeChegada() {
        FilaFEFO fefo = new FilaFEFO();
        fefo.inserir(bolsa("a", "O+", d(20261201)));
        fefo.inserir(bolsa("b", "O+", d(20261201)));
        assertEquals("a", fefo.retirarProximaAVencer().getCodigo());
    }

    // ---------- 3) Hash ----------

    @Test
    void hash_mesmosIndicesDaVersaoCpp() {
        TabelaHashEstoque e = new TabelaHashEstoque();
        assertEquals(16, e.funcaoHash("O-"));
        assertEquals(57, e.funcaoHash("A+"));
    }

    @Test
    void hash_buscaPorTipoComFefoInterno() {
        TabelaHashEstoque e = estoque();
        assertEquals(2, e.quantidade("O-"));
        assertNull(e.buscar("AB-"));
        assertEquals("2", e.buscar("O-").proximaAVencer().getCodigo());
    }

    // ---------- 4) Rota ----------

    @Test
    void rota_vizinhoMaisProximo() {
        Rota r = mapa().rotaVizinhoMaisProximo("Hemocentro", "Hospital Municipal Norte");
        assertTrue(r.encontrada());
        assertEquals(List.of("Hemocentro", "Centro", "Hospital Municipal Norte"), r.paradas());
        assertEquals(11, r.distanciaTotal());
    }

    @Test
    void rota_destinoVizinhoDiretoELocalDesconhecido() {
        GrafoRotas m = mapa();
        assertEquals(2, m.rotaVizinhoMaisProximo("Hemocentro", "Hospital Santa Cruz").paradas().size());
        assertFalse(m.rotaVizinhoMaisProximo("Hemocentro", "Inexistente").encontrada());
    }

    // ---------- 5) Integração ----------

    @Test
    void integracao_pedidoCompletoComRota() {
        TabelaHashEstoque e = estoque();
        AlocadorBolsas alocador = new AlocadorBolsas(e, mapa(), "Hemocentro");

        ResultadoAlocacao a1 = alocador.alocar("Hospital Municipal Norte", "A+", 2);
        assertTrue(a1.atendido());
        assertEquals(List.of("3", "4"), a1.bolsas().stream().map(BolsaSangue::getCodigo).toList());
        assertEquals(11, a1.rota().distanciaTotal());

        ResultadoAlocacao a2 = alocador.alocar("Hospital Santa Cruz", "O-", 3);
        assertFalse(a2.atendido());
        assertEquals(2, e.quantidade("O-"), "pedido recusado não retira nada");

        ResultadoAlocacao a3 = alocador.alocar("Hospital Santa Cruz", "B-", 1);
        assertTrue(a3.atendido());
        assertEquals("O-", a3.bolsas().get(0).getTipoSanguineo());
        assertEquals(1, e.quantidade("B+"), "B+ é incompatível com B- e fica no estoque");

        assertFalse(alocador.alocar("Hospital Santa Cruz", "AB-", 2).atendido());
    }
}
