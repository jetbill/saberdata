package com.saberdata.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Página de resultados con un formato JSON estable (independiente de la clase Page de Spring). */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano,
                                long totalElementos, int totalPaginas) {

    public static <E, T> PaginaResponse<T> desde(Page<E> page, Function<E, T> mapper) {
        return new PaginaResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
