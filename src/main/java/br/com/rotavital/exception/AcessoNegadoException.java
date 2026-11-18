package br.com.rotavital.exception;

/** Quem fez o pedido não tem permissão, ex.: hospital inativo ou não cadastrado (vira HTTP 403 na API). */
public class AcessoNegadoException extends RegraNegocioException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
