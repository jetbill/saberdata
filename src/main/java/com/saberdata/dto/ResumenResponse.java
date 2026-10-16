package com.saberdata.dto;

/** Resumen general: total de estudiantes y reparto ALTO/BAJO (porcentajes en 0 si no hay datos). */
public record ResumenResponse(long totalEstudiantes, long rendimientoAlto, long rendimientoBajo,
                              double porcentajeAlto, double porcentajeBajo) {
}
