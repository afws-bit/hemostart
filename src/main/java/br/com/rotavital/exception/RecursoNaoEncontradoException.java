package br.com.rotavital.exception;

/** O registro pedido não existe (vira HTTP 404 na API). */
public class RecursoNaoEncontradoException extends RegraNegocioException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
