package com.saberdata.repository;

import com.saberdata.model.Rendimiento;
import com.saberdata.model.ResultadoSaber11;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Acceso a los resultados Saber 11.
 * Las estadísticas se calculan con consultas agregadas (GROUP BY) en la base de datos:
 * nunca se cargan las ~106.000 filas en memoria. {@link JpaSpecificationExecutor}
 * permite los filtros opcionales del listado paginado.
 */
public interface ResultadoSaber11Repository
        extends JpaRepository<ResultadoSaber11, Long>, JpaSpecificationExecutor<ResultadoSaber11> {

    long countByRendimiento(Rendimiento rendimiento);

    @Query("""
            select new com.saberdata.repository.GrupoConteo(r.famiEstratovivienda, count(r),
                   count(case when r.rendimiento = com.saberdata.model.Rendimiento.ALTO then 1 end))
            from ResultadoSaber11 r
            group by r.famiEstratovivienda
            order by r.famiEstratovivienda
            """)
    List<GrupoConteo> contarPorEstrato();

    @Query("""
            select new com.saberdata.repository.GrupoConteo(r.coleJornada, count(r),
                   count(case when r.rendimiento = com.saberdata.model.Rendimiento.ALTO then 1 end))
            from ResultadoSaber11 r
            group by r.coleJornada
            order by r.coleJornada
            """)
    List<GrupoConteo> contarPorJornada();

    @Query("""
            select new com.saberdata.repository.GrupoConteo(r.coleNaturaleza, count(r),
                   count(case when r.rendimiento = com.saberdata.model.Rendimiento.ALTO then 1 end))
            from ResultadoSaber11 r
            group by r.coleNaturaleza
            order by r.coleNaturaleza
            """)
    List<GrupoConteo> contarPorNaturaleza();

    @Query("""
            select new com.saberdata.repository.GrupoConteo(r.coleDeptoUbicacion, count(r),
                   count(case when r.rendimiento = com.saberdata.model.Rendimiento.ALTO then 1 end))
            from ResultadoSaber11 r
            group by r.coleDeptoUbicacion
            order by r.coleDeptoUbicacion
            """)
    List<GrupoConteo> contarPorDepartamento();

    @Query("""
            select new com.saberdata.repository.GrupoConteoDoble(r.famiTieneinternet, r.famiTienecomputador,
                   count(r),
                   count(case when r.rendimiento = com.saberdata.model.Rendimiento.ALTO then 1 end))
            from ResultadoSaber11 r
            group by r.famiTieneinternet, r.famiTienecomputador
            order by r.famiTieneinternet, r.famiTienecomputador
            """)
    List<GrupoConteoDoble> contarPorInternetComputador();
}
