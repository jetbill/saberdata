package com.saberdata.importacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Carga el CSV del ICFES (Saber 11, 2022-4) en la tabla {@code resultado_saber11}.
 *
 * <ul>
 *   <li>Todo ocurre en una sola transacción: si algo falla, la tabla queda como estaba.</li>
 *   <li>Las filas inválidas (campos de más o de menos, valor demasiado largo, rendimiento distinto de
 *       ALTO/BAJO) se descartan y se informan; no detienen la carga.</li>
 *   <li>Los valores se guardan tal cual vienen (solo se recortan espacios); las consultas agrupan por ellos.</li>
 * </ul>
 */
@Service
public class ImportadorSaber11 {

    private static final Logger log = LoggerFactory.getLogger(ImportadorSaber11.class);

    /** Columna de destino y su longitud máxima (la del VARCHAR en V1__esquema.sql). */
    private record Columna(String nombre, int maximo) {}

    private static final List<Columna> COLUMNAS = List.of(
            new Columna("estu_genero", 50),
            new Columna("estu_depto_reside", 100),
            new Columna("fami_estratovivienda", 50),
            new Columna("fami_educacionmadre", 100),
            new Columna("fami_educacionpadre", 100),
            new Columna("fami_tieneinternet", 10),
            new Columna("fami_tienecomputador", 10),
            new Columna("fami_tieneautomovil", 10),
            new Columna("fami_tienelavadora", 10),
            new Columna("fami_cuartoshogar", 30),
            new Columna("fami_personashogar", 30),
            new Columna("cole_naturaleza", 30),
            new Columna("cole_area_ubicacion", 30),
            new Columna("cole_jornada", 30),
            new Columna("cole_caracter", 50),
            new Columna("cole_calendario", 10),
            new Columna("cole_depto_ubicacion", 100),
            new Columna("rendimiento", 10));

    private static final int POS_RENDIMIENTO = COLUMNAS.size() - 1;
    private static final Set<String> RENDIMIENTOS = Set.of("ALTO", "BAJO");
    private static final int TAMANO_LOTE = 5000;
    private static final int MAX_EJEMPLOS_DESCARTADOS = 10;

    private static final String INSERT = "INSERT INTO resultado_saber11 ("
            + String.join(", ", COLUMNAS.stream().map(Columna::nombre).toList())
            + ") VALUES (" + "?, ".repeat(COLUMNAS.size() - 1) + "?)";

    private static final int[] TIPOS = new int[COLUMNAS.size()];

    static {
        Arrays.fill(TIPOS, Types.VARCHAR);
    }

    /** Resumen de una importación. {@code ejemplosDescartados} guarda las primeras filas rechazadas. */
    public record Resultado(long leidas, long insertadas, long descartadas, List<String> ejemplosDescartados) {}

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaccion;

    public ImportadorSaber11(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    /**
     * @param archivo    CSV con encabezado (las 18 columnas, en cualquier orden).
     * @param reemplazar si la tabla ya tiene filas: {@code true} las borra antes de cargar;
     *                   {@code false} aborta sin tocar nada.
     */
    public Resultado importar(Path archivo, boolean reemplazar) {
        if (!Files.isRegularFile(archivo)) {
            throw new IllegalArgumentException("No existe el archivo: " + archivo.toAbsolutePath());
        }
        return transaccion.execute(status -> {
            Long existentes = jdbc.queryForObject("SELECT count(*) FROM resultado_saber11", Long.class);
            if (existentes != null && existentes > 0) {
                if (!reemplazar) {
                    throw new IllegalStateException("La tabla resultado_saber11 ya tiene " + existentes
                            + " filas. Para reemplazarlas usa --app.importar.reemplazar=true.");
                }
                log.warn("Borrando las {} filas existentes antes de importar.", existentes);
                jdbc.execute("TRUNCATE TABLE resultado_saber11 RESTART IDENTITY");
            }
            try (BufferedReader in = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                return cargar(in);
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo leer " + archivo, e);
            }
        });
    }

    private Resultado cargar(BufferedReader in) throws IOException {
        String encabezado = in.readLine();
        if (encabezado == null) {
            throw new IllegalArgumentException("El archivo está vacío.");
        }
        int[] posicion = posicionesDeColumnas(encabezado);
        int camposEsperados = encabezado.split(",", -1).length;

        List<Object[]> lote = new ArrayList<>(TAMANO_LOTE);
        List<String> ejemplos = new ArrayList<>();
        long leidas = 0;
        long insertadas = 0;
        long descartadas = 0;
        long numeroLinea = 1;

        String linea;
        while ((linea = in.readLine()) != null) {
            numeroLinea++;
            if (linea.isBlank()) {
                continue;
            }
            leidas++;
            Object[] fila = new Object[COLUMNAS.size()];
            String motivo = convertir(linea, camposEsperados, posicion, fila);
            if (motivo != null) {
                descartadas++;
                if (ejemplos.size() < MAX_EJEMPLOS_DESCARTADOS) {
                    ejemplos.add("línea " + numeroLinea + ": " + motivo);
                }
                continue;
            }
            lote.add(fila);
            if (lote.size() == TAMANO_LOTE) {
                insertadas += insertar(lote);
                log.info("Importadas {} filas...", insertadas);
            }
        }
        insertadas += insertar(lote);
        return new Resultado(leidas, insertadas, descartadas, ejemplos);
    }

    /** Llena {@code fila}; devuelve el motivo del descarte o {@code null} si la fila es válida. */
    private String convertir(String linea, int camposEsperados, int[] posicion, Object[] fila) {
        String[] campos = linea.split(",", -1);
        if (campos.length != camposEsperados) {
            return "tiene " + campos.length + " campos y se esperaban " + camposEsperados;
        }
        for (int i = 0; i < COLUMNAS.size(); i++) {
            Columna columna = COLUMNAS.get(i);
            String valor = campos[posicion[i]].trim();
            if (i == POS_RENDIMIENTO) {
                valor = valor.toUpperCase(Locale.ROOT);
                if (!RENDIMIENTOS.contains(valor)) {
                    return "rendimiento inválido '" + valor + "'";
                }
            }
            if (valor.length() > columna.maximo()) {
                return columna.nombre() + " excede " + columna.maximo() + " caracteres";
            }
            fila[i] = valor.isEmpty() ? null : valor;
        }
        return null;
    }

    private int insertar(List<Object[]> lote) {
        if (lote.isEmpty()) {
            return 0;
        }
        int total = lote.size();
        jdbc.batchUpdate(INSERT, lote, TIPOS);
        lote.clear();
        return total;
    }

    /** Para cada columna de destino, en qué posición viene en el CSV (el orden del archivo no importa). */
    private int[] posicionesDeColumnas(String encabezado) {
        String limpio = encabezado.startsWith("﻿") ? encabezado.substring(1) : encabezado;
        List<String> nombres = Arrays.stream(limpio.split(",", -1))
                .map(n -> n.trim().toLowerCase(Locale.ROOT))
                .toList();
        int[] posicion = new int[COLUMNAS.size()];
        List<String> faltantes = new ArrayList<>();
        for (int i = 0; i < COLUMNAS.size(); i++) {
            posicion[i] = nombres.indexOf(COLUMNAS.get(i).nombre());
            if (posicion[i] < 0) {
                faltantes.add(COLUMNAS.get(i).nombre());
            }
        }
        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException("Faltan columnas en el encabezado del CSV: " + faltantes);
        }
        return posicion;
    }
}
