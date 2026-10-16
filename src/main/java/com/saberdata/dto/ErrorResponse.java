package com.saberdata.dto;

import java.time.Instant;
import java.util.List;

/** Formato uniforme de error JSON para toda la API. */
public record ErrorResponse(Instant timestamp, int status, String error, String mensaje,
                            String ruta, List<String> detalles) {

    public static ErrorResponse of(int status, String error, String mensaje, String ruta) {
        return new ErrorResponse(Instant.now(), status, error, mensaje, ruta, List.of());
    }

    public static ErrorResponse of(int status, String error, String mensaje, String ruta, List<String> detalles) {
        return new ErrorResponse(Instant.now(), status, error, mensaje, ruta, detalles);
    }
}
