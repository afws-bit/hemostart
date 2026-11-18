package br.com.rotavital.dto.api;

import br.com.rotavital.model.Doador;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/** Entrada de POST /api/v1/doadores. A API nunca recebe a entidade diretamente. */
public record DoadorRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotBlank(message = "cpf é obrigatório") String cpf,
        @NotNull(message = "dataNascimento é obrigatória") LocalDate dataNascimento,
        @NotBlank @Pattern(regexp = "[MF]", message = "sexo deve ser M ou F") String sexo,
        @NotBlank @Pattern(regexp = "(A|B|AB|O)[+-]", message = "tipoSanguineo inválido") String tipoSanguineo,
        @Positive(message = "peso deve ser maior que zero") double peso,
        String contato,
        String historicoSaude,
        boolean autorizacaoResponsavel) {

    public Doador paraEntidade() {
        Doador d = new Doador();
        d.setNome(nome);
        d.setCpf(cpf);
        d.setDataNascimento(dataNascimento);
        d.setSexo(sexo);
        d.setTipoSanguineo(tipoSanguineo);
        d.setPeso(peso);
        d.setContato(contato);
        d.setHistoricoSaude(historicoSaude);
        d.setAutorizacaoResponsavel(autorizacaoResponsavel);
        return d;
    }
}
