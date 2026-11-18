package br.com.rotavital.controller.api;

import br.com.rotavital.dto.api.ErroResponse;
import br.com.rotavital.exception.AcessoNegadoException;
import br.com.rotavital.exception.RecursoNaoEncontradoException;
import br.com.rotavital.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PI3-74 — tratamento GLOBAL de erros da API REST (/api/v1).
 * Toda exceção vira um JSON no mesmo formato (ErroResponse) com o status HTTP certo:
 *   400 dados inválidos · 403 acesso negado · 404 não encontrado · 409 regra de negócio · 500 erro inesperado.
 * Vale só para o pacote controller.api; as telas Thymeleaf continuam mostrando a mensagem na própria página.
 */
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest req) {
        return erro(HttpStatus.NOT_FOUND, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AcessoNegadoException e, HttpServletRequest req) {
        return erro(HttpStatus.FORBIDDEN, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException e, HttpServletRequest req) {
        return erro(HttpStatus.CONFLICT, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<String> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return erro(HttpStatus.BAD_REQUEST, "Dados inválidos", req, detalhes);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroResponse> requisicaoInvalida(Exception e, HttpServletRequest req) {
        String mensagem = e instanceof HttpMessageNotReadableException
                ? "Corpo da requisição ausente ou em formato inválido"
                : e.getMessage();
        return erro(HttpStatus.BAD_REQUEST, mensagem, req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception e, HttpServletRequest req) {
        log.error("Erro inesperado na API em {}", req.getRequestURI(), e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente mais tarde.", req, List.of());
    }

    private ResponseEntity<ErroResponse> erro(HttpStatus status, String mensagem, HttpServletRequest req,
                                              List<String> detalhes) {
        return ResponseEntity.status(status).body(new ErroResponse(LocalDateTime.now(), status.value(),
                status.getReasonPhrase(), mensagem, req.getRequestURI(), detalhes));
    }
}
