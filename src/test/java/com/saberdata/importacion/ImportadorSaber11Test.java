package com.saberdata.importacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba el importador contra PostgreSQL 16 real. Se omite si Docker no está activo. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Import(ImportadorSaber11.class)
class ImportadorSaber11Test {

    private static final String ENCABEZADO = "estu_genero,estu_depto_reside,fami_estratovivienda,"
            + "fami_educacionmadre,fami_educacionpadre,fami_tieneinternet,fami_tienecomputador,"
            + "fami_tieneautomovil,fami_tienelavadora,fami_cuartoshogar,fami_personashogar,"
            + "cole_naturaleza,cole_area_ubicacion,cole_jornada,cole_caracter,cole_calendario,"
            + "cole_depto_ubicacion,rendimiento";

    private static final String FILA_ALTO =
            "M,ANTIOQUIA,Estrato 2,Primaria incompleta,Primaria incompleta,No,No,No,No,Dos,3 a 4,"
                    + "OFICIAL,RURAL,COMPLETA,NO APLICA,A,ANTIOQUIA,ALTO";
    private static final String FILA_BAJO =
            "F,VALLE,Estrato 3,No sabe,No sabe,Si,Si,No,Si,Tres,5 a 6,NO OFICIAL,URBANO,MANANA,ACADEMICO,A,VALLE,BAJO";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired
    ImportadorSaber11 importador;

    @Autowired
    JdbcTemplate jdbc;

    @TempDir
    Path tmp;

    @Test
    void importaFilasValidasYDescartaLasInvalidas() throws IOException {
        Path csv = csv(FILA_ALTO,
                FILA_BAJO,
                FILA_ALTO.replace(",ALTO", ",REGULAR"),   // rendimiento inválido
                "M,ANTIOQUIA,Estrato 2",                  // faltan campos
                "");                                      // línea en blanco: se ignora

        ImportadorSaber11.Resultado r = importador.importar(csv, false);

        assertThat(r.leidas()).isEqualTo(4);
        assertThat(r.insertadas()).isEqualTo(2);
        assertThat(r.descartadas()).isEqualTo(2);
        assertThat(r.ejemplosDescartados()).hasSize(2);
        assertThat(contar()).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM resultado_saber11 WHERE rendimiento = 'ALTO'", Long.class)).isEqualTo(1);
    }

    @Test
    void siLaTablaTieneDatos_seNiegaSalvoQueSePidaReemplazar() throws IOException {
        Path csv = csv(FILA_ALTO, FILA_BAJO);
        importador.importar(csv, false);

        assertThatThrownBy(() -> importador.importar(csv, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya tiene 2 filas");

        importador.importar(csv(FILA_BAJO), true);
        assertThat(contar()).isEqualTo(1);
    }

    @Test
    void siFaltaUnaColumna_fallaConMensajeClaro() throws IOException {
        Path csv = tmp.resolve("sin-columna.csv");
        Files.writeString(csv, ENCABEZADO.replace(",rendimiento", "") + "\n");

        assertThatThrownBy(() -> importador.importar(csv, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rendimiento");
    }

    @Test
    void siElArchivoNoExiste_fallaConMensajeClaro() {
        assertThatThrownBy(() -> importador.importar(tmp.resolve("no-existe.csv"), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No existe el archivo");
    }

    private Path csv(String... filas) throws IOException {
        Path archivo = tmp.resolve("datos.csv");
        Files.writeString(archivo, ENCABEZADO + "\n" + String.join("\n", filas) + "\n");
        return archivo;
    }

    private long contar() {
        return jdbc.queryForObject("SELECT count(*) FROM resultado_saber11", Long.class);
    }
}
