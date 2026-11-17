package br.com.rotavital.exception;

/**
 * Lançada quando uma regra de negócio do banco de sangue é violada
 * (ex.: CPF duplicado, bolsa já distribuída, hospital inativo).
 * A mensagem é exibida diretamente para o usuário na interface web.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
