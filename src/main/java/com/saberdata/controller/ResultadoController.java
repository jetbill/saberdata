package com.saberdata.controller;

import com.saberdata.dto.PaginaResponse;
import com.saberdata.dto.ResultadoResponse;
import com.saberdata.model.Rendimiento;
import com.saberdata.service.ResultadoService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resultados")
public class ResultadoController {

    private final ResultadoService service;

    public ResultadoController(ResultadoService service) {
        this.service = service;
    }

    /** Ejemplo: {@code /api/resultados?estrato=Estrato 3&rendimiento=ALTO&page=0&size=20}. */
    @GetMapping
    public PaginaResponse<ResultadoResponse> listar(
            @RequestParam(name = "estrato", required = false) String estrato,
            @RequestParam(name = "jornada", required = false) String jornada,
            @RequestParam(name = "naturaleza", required = false) String naturaleza,
            @RequestParam(name = "departamento", required = false) String departamento,
            @RequestParam(name = "rendimiento", required = false) Rendimiento rendimiento,
            Pageable pageable) {
        return service.buscar(estrato, jornada, naturaleza, departamento, rendimiento, pageable);
    }
}
