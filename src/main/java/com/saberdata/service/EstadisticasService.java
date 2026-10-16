package com.saberdata.service;

import com.saberdata.dto.GrupoRendimientoResponse;
import com.saberdata.dto.ResumenResponse;
import com.saberdata.model.Rendimiento;
import com.saberdata.repository.GrupoConteo;
import com.saberdata.repository.GrupoConteoDoble;
import com.saberdata.repository.ResultadoSaber11Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Estadísticas de rendimiento. Todo se calcula en la base de datos con consultas agregadas;
 * con la tabla vacía devuelve ceros y listas vacías (nunca divide por cero).
 * También lo consumen las herramientas del chatbot (EstadisticasTools).
 */
@Service
@Transactional(readOnly = true)
public class EstadisticasService {

    private static final String SIN_DATO = "SIN_DATO";

    private final ResultadoSaber11Repository repository;

    public EstadisticasService(ResultadoSaber11Repository repository) {
        this.repository = repository;
    }

    public ResumenResponse resumen() {
        long alto = repository.countByRendimiento(Rendimiento.ALTO);
        long bajo = repository.countByRendimiento(Rendimiento.BAJO);
        long total = repository.count();
        return new ResumenResponse(total, alto, bajo, porcentaje(alto, total), porcentaje(bajo, total));
    }

    public List<GrupoRendimientoResponse> rendimientoPorEstrato() {
        return convertir(repository.contarPorEstrato());
    }

    public List<GrupoRendimientoResponse> rendimientoPorJornada() {
        return convertir(repository.contarPorJornada());
    }

    public List<GrupoRendimientoResponse> rendimientoPorNaturaleza() {
        return convertir(repository.contarPorNaturaleza());
    }

    public List<GrupoRendimientoResponse> rendimientoPorDepartamento() {
        return convertir(repository.contarPorDepartamento());
    }

    /** Cruza "tiene internet" y "tiene computador"; el grupo se etiqueta, p. ej. "Internet: SI | Computador: NO". */
    public List<GrupoRendimientoResponse> rendimientoPorInternetComputador() {
        return repository.contarPorInternetComputador().stream()
                .map(c -> grupo("Internet: " + valor(c.primero()) + " | Computador: " + valor(c.segundo()),
                        c.total(), c.alto()))
                .toList();
    }

    private List<GrupoRendimientoResponse> convertir(List<GrupoConteo> conteos) {
        return conteos.stream().map(c -> grupo(valor(c.grupo()), c.total(), c.alto())).toList();
    }

    private GrupoRendimientoResponse grupo(String nombre, long total, long alto) {
        long bajo = total - alto;
        return new GrupoRendimientoResponse(nombre, total, alto, bajo, porcentaje(alto, total), porcentaje(bajo, total));
    }

    private String valor(String texto) {
        return (texto == null || texto.isBlank()) ? SIN_DATO : texto;
    }

    /** Porcentaje con 2 decimales; 0 si el total es 0. */
    static double porcentaje(long parte, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round(parte * 10000.0 / total) / 100.0;
    }
}
