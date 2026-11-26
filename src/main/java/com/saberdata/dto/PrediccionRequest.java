package com.saberdata.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Datos de un estudiante para predecir su rendimiento. Las claves son las de {@code GET /api/mineria/atributos}
 * (por ejemplo {@code estrato}, {@code jornada}); todas son opcionales, y un atributo ausente se trata como dato faltante.
 */
public record PrediccionRequest(@NotEmpty @Size(max = 40) Map<String, String> atributos) {
}
