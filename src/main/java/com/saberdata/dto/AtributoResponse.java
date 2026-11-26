package com.saberdata.dto;

import java.util.List;

/** Un atributo del modelo con los valores que conoce (sirve para construir el formulario de predicción). */
public record AtributoResponse(String clave, String etiqueta, List<String> valores) {
}
