package com.saberdata.dto;

import com.saberdata.mineria.Metricas;

import java.time.Instant;

/** Estado del modelo de minería de datos. Si aún no se entrenó, {@code disponible} es false y el resto es nulo. */
public record ModeloResponse(boolean disponible, Instant entrenadoEn, String algoritmo, Metricas metricas) {

    public static ModeloResponse noDisponible() {
        return new ModeloResponse(false, null, null, null);
    }
}
