package com.saberdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Minería de datos ({@code app.mineria.*}).
 *
 * @param modeloRuta archivo donde se guarda el modelo entrenado (se crea al entrenar)
 * @param pliegues   número de pliegues de la validación cruzada
 * @param minObj     mínimo de instancias por hoja del árbol J48; más alto = árbol más pequeño y fácil de leer
 */
@ConfigurationProperties(prefix = "app.mineria")
public record MineriaProperties(String modeloRuta, int pliegues, int minObj) {

    public MineriaProperties {
        if (modeloRuta == null || modeloRuta.isBlank()) {
            throw new IllegalStateException("MINERIA_MODELO_RUTA es obligatorio.");
        }
        if (pliegues < 2) {
            throw new IllegalStateException("MINERIA_PLIEGUES debe ser al menos 2.");
        }
        if (minObj < 1) {
            throw new IllegalStateException("MINERIA_MIN_OBJ debe ser al menos 1.");
        }
    }
}
