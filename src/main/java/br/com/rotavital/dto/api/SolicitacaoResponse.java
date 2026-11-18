package br.com.rotavital.dto.api;

import br.com.rotavital.model.Requisicao;

import java.time.LocalDateTime;

/** Saída da API para solicitação de sangue. */
public record SolicitacaoResponse(
        String id,
        String hospitalId,
        String hospital,
        String tipoSanguineo,
        int quantidade,
        boolean urgente,
        String status,
        LocalDateTime dataSolicitacao,
        LocalDateTime dataAtendimento,
        String rotaEntrega,
        Integer distanciaKm,
        String motivoRecusa) {

    public static SolicitacaoResponse de(Requisicao r) {
        return new SolicitacaoResponse(r.getId(), r.getHospitalId(),
                r.getHospital() != null ? r.getHospital().getRazaoSocial() : null,
                r.getTipoSanguineo(), r.getQuantidade(), r.isUrgente(), r.getStatus().getChave(),
                r.getDataSolicitacao(), r.getDataAtendimento(), r.getRotaEntrega(), r.getDistanciaKm(),
                r.getMotivoRecusa());
    }
}
