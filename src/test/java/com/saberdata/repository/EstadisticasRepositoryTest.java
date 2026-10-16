package com.saberdata.repository;

import com.saberdata.dto.GrupoRendimientoResponse;
import com.saberdata.dto.ResumenResponse;
import com.saberdata.model.Rendimiento;
import com.saberdata.model.ResultadoSaber11;
import com.saberdata.service.EstadisticasService;
import com.saberdata.service.ResultadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba el repositorio y las consultas agregadas contra un PostgreSQL 16 real (Testcontainers).
 * Flyway crea el esquema y Hibernate lo valida (ddl-auto=validate). Se omite si Docker no está activo.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import({EstadisticasService.class, ResultadoService.class})
class EstadisticasRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired
    ResultadoSaber11Repository repository;

    @Autowired
    EstadisticasService estadisticas;

    @Autowired
    ResultadoService resultados;

    @Test
    void tablaVacia_devuelveCerosYListasVacias() {
        ResumenResponse r = estadisticas.resumen();
        assertThat(r.totalEstudiantes()).isZero();
        assertThat(r.porcentajeAlto()).isZero();
        assertThat(r.porcentajeBajo()).isZero();
        assertThat(estadisticas.rendimientoPorEstrato()).isEmpty();
        assertThat(estadisticas.rendimientoPorInternetComputador()).isEmpty();
        assertThat(resultados.buscar(null, null, null, null, null, PageRequest.of(0, 10)).totalElementos())
                .isZero();
    }

    @Test
    void calculaPorcentajesPorEstratoYFiltra() {
        // Estrato 1: 3 estudiantes (2 ALTO, 1 BAJO). Estrato 3: 1 estudiante BAJO.
        repository.saveAll(List.of(
                fila("Estrato 1", "COMPLETA", "OFICIAL", "SI", "SI", Rendimiento.ALTO),
                fila("Estrato 1", "MANANA", "OFICIAL", "SI", "NO", Rendimiento.ALTO),
                fila("Estrato 1", "MANANA", "NO OFICIAL", "NO", "NO", Rendimiento.BAJO),
                fila("Estrato 3", "COMPLETA", "NO OFICIAL", "SI", "SI", Rendimiento.BAJO)));

        ResumenResponse r = estadisticas.resumen();
        assertThat(r.totalEstudiantes()).isEqualTo(4);
        assertThat(r.rendimientoAlto()).isEqualTo(2);
        assertThat(r.porcentajeAlto()).isEqualTo(50.0);

        GrupoRendimientoResponse estrato1 = estadisticas.rendimientoPorEstrato().stream()
                .filter(g -> g.grupo().equals("Estrato 1")).findFirst().orElseThrow();
        assertThat(estrato1.total()).isEqualTo(3);
        assertThat(estrato1.alto()).isEqualTo(2);
        assertThat(estrato1.bajo()).isEqualTo(1);
        assertThat(estrato1.porcentajeAlto()).isEqualTo(66.67);

        assertThat(estadisticas.rendimientoPorJornada()).hasSize(2);
        assertThat(estadisticas.rendimientoPorNaturaleza()).hasSize(2);
        assertThat(estadisticas.rendimientoPorDepartamento()).hasSize(1);
        assertThat(estadisticas.rendimientoPorInternetComputador()).hasSize(3);

        var filtrado = resultados.buscar("Estrato 1", null, null, null, Rendimiento.ALTO, PageRequest.of(0, 10));
        assertThat(filtrado.totalElementos()).isEqualTo(2);
    }

    private ResultadoSaber11 fila(String estrato, String jornada, String naturaleza,
                                  String internet, String computador, Rendimiento rendimiento) {
        ResultadoSaber11 r = new ResultadoSaber11();
        r.setFamiEstratovivienda(estrato);
        r.setColeJornada(jornada);
        r.setColeNaturaleza(naturaleza);
        r.setFamiTieneinternet(internet);
        r.setFamiTienecomputador(computador);
        r.setColeDeptoUbicacion("ANTIOQUIA");
        r.setRendimiento(rendimiento);
        return r;
    }
}
