package com.saberdata.mineria;

import com.saberdata.config.MineriaProperties;
import com.saberdata.dto.PrediccionResponse;
import com.saberdata.exception.ApiException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba el entrenamiento con J48, la predicción y el guardado del modelo con datos sintéticos, sin base de datos:
 * el rendimiento depende (con un 5 % de ruido) de si el estrato es 3 o 4.
 */
class MineriaServiceTest {

    private static List<String[]> filas;

    private final MineriaProperties props = new MineriaProperties("no-se-usa.model", 5, 5);

    @BeforeAll
    static void datosDeEjemplo() {
        Random azar = new Random(42);
        String[] estratos = {"Estrato 1", "Estrato 2", "Estrato 3", "Estrato 4"};
        String[] jornadas = {"COMPLETA", "MANANA", "TARDE"};
        int n = CatalogoAtributos.TODOS.size();
        filas = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            String[] fila = new String[n + 1];
            for (int c = 0; c < n; c++) {
                fila[c] = "v" + azar.nextInt(3);
            }
            String estrato = estratos[azar.nextInt(estratos.length)];
            fila[indice("estrato")] = estrato;
            fila[indice("jornada")] = jornadas[azar.nextInt(jornadas.length)];
            boolean alto = estrato.equals("Estrato 3") || estrato.equals("Estrato 4");
            if (azar.nextInt(100) < 5) {
                alto = !alto;
            }
            fila[n] = alto ? "ALTO" : "BAJO";
            filas.add(fila);
        }
    }

    private static int indice(String clave) {
        for (int i = 0; i < CatalogoAtributos.TODOS.size(); i++) {
            if (CatalogoAtributos.TODOS.get(i).clave().equals(clave)) {
                return i;
            }
        }
        throw new IllegalArgumentException(clave);
    }

    private MineriaService servicioEntrenado() {
        MineriaService servicio = new MineriaService(null, props);
        servicio.instalar(servicio.entrenarConFilas(filas));
        return servicio;
    }

    @Test
    void aprendeLaReglaYLaSuperaAlAzar() {
        ModeloEntrenado modelo = new MineriaService(null, props).entrenarConFilas(filas);
        Metricas m = modelo.metricas();

        assertThat(m.instancias()).isEqualTo(600);
        assertThat(m.atributos()).isEqualTo(CatalogoAtributos.TODOS.size());
        assertThat(m.exactitud()).isGreaterThan(0.9).isGreaterThan(m.exactitudBase());
        assertThat(m.auc()).isGreaterThan(0.9);
        assertThat(m.matrizConfusion()).hasDimensions(2, 2);
        assertThat(modelo.arbol()).contains("estrato");
    }

    @Test
    void predice_segunElEstrato() {
        MineriaService servicio = servicioEntrenado();

        PrediccionResponse alto = servicio.predecir(Map.of("estrato", "Estrato 3"));
        PrediccionResponse bajo = servicio.predecir(Map.of("estrato", "Estrato 1"));

        assertThat(alto.prediccion()).isEqualTo("ALTO");
        assertThat(alto.probabilidadAlto()).isGreaterThan(0.7);
        assertThat(bajo.prediccion()).isEqualTo("BAJO");
        assertThat(bajo.probabilidadBajo()).isGreaterThan(0.7);
        assertThat(alto.probabilidadAlto() + alto.probabilidadBajo()).isBetween(0.999, 1.001);
    }

    @Test
    void valorQueElModeloNoConoce_seIgnoraYSeAvisa() {
        PrediccionResponse r = servicioEntrenado().predecir(Map.of("estrato", "Estrato 99", "jornada", "COMPLETA"));

        assertThat(r.ignorados()).containsExactly("estrato");
        assertThat(r.probabilidadAlto() + r.probabilidadBajo()).isBetween(0.999, 1.001);
    }

    @Test
    void atributoDesconocido_responde400() {
        MineriaService servicio = servicioEntrenado();

        assertThatThrownBy(() -> servicio.predecir(Map.of("colorFavorito", "azul")))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void sinModeloEntrenado_responde503() {
        MineriaService servicio = new MineriaService(null, props);

        assertThat(servicio.estado().disponible()).isFalse();
        assertThatThrownBy(() -> servicio.predecir(Map.of("estrato", "Estrato 3")))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void guardadoYLecturaConservanElModelo(@TempDir Path carpeta) throws Exception {
        MineriaService original = servicioEntrenado();
        Path archivo = carpeta.resolve("modelo.model");
        original.guardar(original.entrenarConFilas(filas), archivo);

        MineriaService recargado = new MineriaService(null, props);
        recargado.instalar(MineriaService.leer(archivo));

        PrediccionResponse r = recargado.predecir(Map.of("estrato", "Estrato 4"));
        assertThat(r.prediccion()).isEqualTo("ALTO");
        assertThat(recargado.estado().disponible()).isTrue();
        assertThat(recargado.atributos()).hasSize(CatalogoAtributos.TODOS.size());
    }
}
