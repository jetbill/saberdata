package com.saberdata.importacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Ejecuta la importación una sola vez y cierra la aplicación. Solo existe con el perfil {@code importar}:
 * <pre>./gradlew bootRun --args='--spring.profiles.active=importar'</pre>
 * Si algo falla, la excepción detiene el arranque (código de salida distinto de 0).
 */
@Component
@Profile("importar")
class ImportadorRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportadorRunner.class);

    private final ImportadorSaber11 importador;
    private final ConfigurableApplicationContext contexto;
    private final String archivo;
    private final boolean reemplazar;

    ImportadorRunner(ImportadorSaber11 importador,
                     ConfigurableApplicationContext contexto,
                     @Value("${app.importar.archivo}") String archivo,
                     @Value("${app.importar.reemplazar:false}") boolean reemplazar) {
        this.importador = importador;
        this.contexto = contexto;
        this.archivo = archivo;
        this.reemplazar = reemplazar;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path ruta = Path.of(archivo);
        log.info("Importando {} (reemplazar={})...", ruta.toAbsolutePath(), reemplazar);
        long inicio = System.currentTimeMillis();

        ImportadorSaber11.Resultado r = importador.importar(ruta, reemplazar);

        log.info("Importación terminada en {} s: {} filas leídas, {} insertadas, {} descartadas.",
                (System.currentTimeMillis() - inicio) / 1000, r.leidas(), r.insertadas(), r.descartadas());
        r.ejemplosDescartados().forEach(e -> log.warn("Fila descartada, {}", e));

        SpringApplication.exit(contexto, () -> 0);
    }
}
