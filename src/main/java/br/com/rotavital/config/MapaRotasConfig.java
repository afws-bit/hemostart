package br.com.rotavital.config;

import br.com.rotavital.aed.GrafoRotas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mapa logístico usado pela roteirização (AED U2): hemocentro, pontos de passagem
 * e os hospitais cadastrados nos dados iniciais. Distâncias em km.
 * Para incluir um hospital novo na roteirização, adicione aqui uma rota com o
 * mesmo nome (razão social) usado no cadastro.
 */
@Configuration
public class MapaRotasConfig {

    public static final String HEMOCENTRO = "Hemocentro";

    @Bean
    public GrafoRotas grafoRotas() {
        GrafoRotas mapa = new GrafoRotas();
        mapa.adicionarRota(HEMOCENTRO, "Centro", 4);
        mapa.adicionarRota(HEMOCENTRO, "Hospital Santa Cruz", 12);
        mapa.adicionarRota("Centro", "Hospital Municipal Norte", 7);
        mapa.adicionarRota("Centro", "Zona Sul", 5);
        mapa.adicionarRota("Zona Sul", "Hospital Santa Cruz", 6);
        mapa.adicionarRota("Zona Sul", "Clínica São Lucas (Inativa)", 9);
        return mapa;
    }
}
