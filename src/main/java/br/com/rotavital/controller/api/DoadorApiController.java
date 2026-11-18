package br.com.rotavital.controller.api;

import br.com.rotavital.dto.api.DoadorRequest;
import br.com.rotavital.dto.api.DoadorResponse;
import br.com.rotavital.service.DoadorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Contrato de API — doadores (HU01). */
@RestController
@RequestMapping("/api/v1/doadores")
public class DoadorApiController {

    private final DoadorService doadorService;

    public DoadorApiController(DoadorService doadorService) {
        this.doadorService = doadorService;
    }

    /** 201 criado · 400 dados inválidos · 409 regra de negócio (ex.: CPF já cadastrado). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DoadorResponse cadastrar(@Valid @RequestBody DoadorRequest request) {
        return DoadorResponse.de(doadorService.cadastrar(request.paraEntidade()));
    }

    /** 200 · 404 doador não encontrado. */
    @GetMapping("/{id}")
    public DoadorResponse buscar(@PathVariable("id") String id) {
        return DoadorResponse.de(doadorService.buscarPorId(id));
    }
}
