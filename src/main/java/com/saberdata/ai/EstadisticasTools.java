package com.saberdata.ai;

import com.saberdata.dto.GrupoRendimientoResponse;
import com.saberdata.dto.ResumenResponse;
import com.saberdata.service.EstadisticasService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Herramientas (tool calling) que el LLM puede invocar para consultar datos REALES de la base.
 * Así el modelo no inventa cifras: llama a estos métodos y responde con lo que devuelven.
 * Devuelven texto compacto, más fácil de usar para un modelo local pequeño.
 */
@Component
public class EstadisticasTools {

    private static final String SIN_DATOS = "Aún no hay datos de estudiantes cargados en la base de datos.";

    private final EstadisticasService estadisticas;

    public EstadisticasTools(EstadisticasService estadisticas) {
        this.estadisticas = estadisticas;
    }

    @Tool(description = "Resumen general de Saber 11: total de estudiantes y porcentaje con rendimiento ALTO y BAJO.")
    public String resumenGeneral() {
        ResumenResponse r = estadisticas.resumen();
        if (r.totalEstudiantes() == 0) {
            return SIN_DATOS;
        }
        return String.format(Locale.US, "Total: %d estudiantes. ALTO: %d (%.2f%%). BAJO: %d (%.2f%%).",
                r.totalEstudiantes(), r.rendimientoAlto(), r.porcentajeAlto(),
                r.rendimientoBajo(), r.porcentajeBajo());
    }

    @Tool(description = "Porcentaje de rendimiento ALTO y BAJO según el estrato socioeconómico de la vivienda.")
    public String rendimientoPorEstrato() {
        return formatear(estadisticas.rendimientoPorEstrato());
    }

    @Tool(description = "Porcentaje de rendimiento ALTO y BAJO según la jornada del colegio (mañana, tarde, completa...).")
    public String rendimientoPorJornada() {
        return formatear(estadisticas.rendimientoPorJornada());
    }

    @Tool(description = "Porcentaje de rendimiento ALTO y BAJO según la naturaleza del colegio (oficial o no oficial).")
    public String rendimientoPorNaturaleza() {
        return formatear(estadisticas.rendimientoPorNaturaleza());
    }

    @Tool(description = "Porcentaje de rendimiento ALTO y BAJO por departamento donde está ubicado el colegio.")
    public String rendimientoPorDepartamento() {
        return formatear(estadisticas.rendimientoPorDepartamento());
    }

    @Tool(description = "Porcentaje de rendimiento ALTO y BAJO según si el hogar tiene internet y computador.")
    public String rendimientoPorInternetComputador() {
        return formatear(estadisticas.rendimientoPorInternetComputador());
    }

    private String formatear(List<GrupoRendimientoResponse> grupos) {
        if (grupos.isEmpty()) {
            return SIN_DATOS;
        }
        return grupos.stream()
                .map(g -> String.format(Locale.US, "%s: %d estudiantes, ALTO %.2f%%, BAJO %.2f%%",
                        g.grupo(), g.total(), g.porcentajeAlto(), g.porcentajeBajo()))
                .collect(Collectors.joining("\n"));
    }
}
