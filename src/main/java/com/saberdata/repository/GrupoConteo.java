package com.saberdata.repository;

/** Proyección de una consulta agregada: estudiantes de un grupo y cuántos tienen rendimiento ALTO. */
public record GrupoConteo(String grupo, Long total, Long alto) {
}
