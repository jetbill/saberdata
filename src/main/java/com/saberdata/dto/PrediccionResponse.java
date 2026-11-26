package com.saberdata.dto;

import java.util.List;

/**
 * @param prediccion        clase más probable: {@code ALTO} o {@code BAJO}
 * @param ignorados         atributos cuyo valor no existía al entrenar el modelo y se trataron como dato faltante
 */
public record PrediccionResponse(String prediccion, double probabilidadAlto, double probabilidadBajo,
                                 List<String> ignorados) {
}
