package com.saberdata.controller;

import com.saberdata.dto.GrupoRendimientoResponse;
import com.saberdata.dto.ResumenResponse;
import com.saberdata.service.EstadisticasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Datos agregados listos para graficar en el frontend. */
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final EstadisticasService service;

    public EstadisticasController(EstadisticasService service) {
        this.service = service;
    }

    @GetMapping("/resumen")
    public ResumenResponse resumen() {
        return service.resumen();
    }

    @GetMapping("/rendimiento-por-estrato")
    public List<GrupoRendimientoResponse> porEstrato() {
        return service.rendimientoPorEstrato();
    }

    @GetMapping("/rendimiento-por-jornada")
    public List<GrupoRendimientoResponse> porJornada() {
        return service.rendimientoPorJornada();
    }

    @GetMapping("/rendimiento-por-naturaleza")
    public List<GrupoRendimientoResponse> porNaturaleza() {
        return service.rendimientoPorNaturaleza();
    }

    @GetMapping("/rendimiento-por-departamento")
    public List<GrupoRendimientoResponse> porDepartamento() {
        return service.rendimientoPorDepartamento();
    }

    @GetMapping("/rendimiento-por-internet-computador")
    public List<GrupoRendimientoResponse> porInternetComputador() {
        return service.rendimientoPorInternetComputador();
    }
}
