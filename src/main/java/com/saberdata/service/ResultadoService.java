package com.saberdata.service;

import com.saberdata.dto.PaginaResponse;
import com.saberdata.dto.ResultadoResponse;
import com.saberdata.model.Rendimiento;
import com.saberdata.model.ResultadoSaber11;
import com.saberdata.repository.ResultadoSaber11Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listado paginado de resultados con filtros opcionales. */
@Service
@Transactional(readOnly = true)
public class ResultadoService {

    private final ResultadoSaber11Repository repository;

    public ResultadoService(ResultadoSaber11Repository repository) {
        this.repository = repository;
    }

    public PaginaResponse<ResultadoResponse> buscar(String estrato, String jornada, String naturaleza,
                                                   String departamento, Rendimiento rendimiento,
                                                   Pageable pageable) {
        // Cada filtro presente se añade con AND; los ausentes se ignoran.
        Specification<ResultadoSaber11> spec = (root, query, cb) -> cb.conjunction();
        spec = spec.and(igual("famiEstratovivienda", estrato));
        spec = spec.and(igual("coleJornada", jornada));
        spec = spec.and(igual("coleNaturaleza", naturaleza));
        spec = spec.and(igual("coleDeptoUbicacion", departamento));
        if (rendimiento != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("rendimiento"), rendimiento));
        }

        // El orden lo fija el servidor (por id): evita ordenar por columnas arbitrarias del cliente.
        Pageable paginacion = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
        Page<ResultadoSaber11> pagina = repository.findAll(spec, paginacion);
        return PaginaResponse.desde(pagina, ResultadoResponse::desde);
    }

    private Specification<ResultadoSaber11> igual(String atributo, String valor) {
        return (root, query, cb) -> (valor == null || valor.isBlank())
                ? cb.conjunction()
                : cb.equal(root.get(atributo), valor.trim());
    }
}
