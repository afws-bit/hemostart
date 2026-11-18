package br.com.rotavital.dto.api;

import br.com.rotavital.model.BolsaSangue;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Saída da API para bolsa de sangue (referências viram ids, nunca entidades aninhadas). */
public record BolsaResponse(
        String codigo,
        String tipoSanguineo,
        String componente,
        int volume,
        String status,
        LocalDate dataColeta,
        LocalDate dataValidade,
        boolean usoPrioritario,
        String doadorId,
        String solicitacaoId,
        String hospitalDestinoId,
        LocalDateTime dataDistribuicao) {

    public static BolsaResponse de(BolsaSangue b) {
        return new BolsaResponse(b.getCodigo(), b.getTipoSanguineo(), b.getComponente(), b.getVolume(),
                b.getStatus().getChave(), b.getDataColeta(), b.getDataValidade(), b.isUsoPrioritario(),
                b.getDoadorId(), b.getRequisicaoId(), b.getHospitalDestinoId(), b.getDataDistribuicao());
    }
}
