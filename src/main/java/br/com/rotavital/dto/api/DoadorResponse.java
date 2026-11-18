package br.com.rotavital.dto.api;

import br.com.rotavital.model.Doador;

import java.time.LocalDate;

/** Saída da API para doador. O CPF vai mascarado e o histórico de saúde não é exposto. */
public record DoadorResponse(
        String id,
        String nome,
        String cpf,
        LocalDate dataNascimento,
        String sexo,
        String tipoSanguineo,
        double peso,
        LocalDate ultimaDoacao,
        boolean aptoParaDoar,
        LocalDate proximaDataApta) {

    public static DoadorResponse de(Doador d) {
        LocalDate hoje = LocalDate.now();
        return new DoadorResponse(d.getId(), d.getNome(), mascararCpf(d.getCpf()), d.getDataNascimento(),
                d.getSexo(), d.getTipoSanguineo(), d.getPeso(), d.getUltimaDoacao(),
                d.aptoPorIntervalo(hoje), d.proximaDataApta(hoje));
    }

    private static String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }
}
