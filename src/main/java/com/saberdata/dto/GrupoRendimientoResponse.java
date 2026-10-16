package com.saberdata.dto;

/** Una barra/segmento de un gráfico: un grupo (estrato, jornada...) con su reparto de rendimiento. */
public record GrupoRendimientoResponse(String grupo, long total, long alto, long bajo,
                                       double porcentajeAlto, double porcentajeBajo) {
}
