package com.saberdata.dto;

/** Árbol J48 en texto. Se recorta a un máximo de líneas para no devolver respuestas enormes. */
public record ArbolResponse(String texto, int lineas, boolean truncado) {
}
