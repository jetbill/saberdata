package com.saberdata.mineria;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Atributos del dataset Saber 11 que usa el modelo de minería de datos.
 * La {@code clave} es el nombre que viaja por la API (igual que en {@code ResultadoResponse});
 * la {@code columna} es la columna de {@code resultado_saber11} de donde se leen los datos.
 */
public final class CatalogoAtributos {

    public record Atributo(String clave, String columna, String etiqueta) {
    }

    /** Variable objetivo. */
    public static final String CLASE = "rendimiento";
    public static final String COLUMNA_CLASE = "rendimiento";
    public static final List<String> VALORES_CLASE = List.of("ALTO", "BAJO");

    /** Atributos predictores, en el orden en que se leen de la base. */
    public static final List<Atributo> TODOS = List.of(
            new Atributo("genero", "estu_genero", "Género"),
            new Atributo("deptoResidencia", "estu_depto_reside", "Departamento de residencia"),
            new Atributo("estrato", "fami_estratovivienda", "Estrato de la vivienda"),
            new Atributo("educacionMadre", "fami_educacionmadre", "Educación de la madre"),
            new Atributo("educacionPadre", "fami_educacionpadre", "Educación del padre"),
            new Atributo("tieneInternet", "fami_tieneinternet", "¿Tiene internet?"),
            new Atributo("tieneComputador", "fami_tienecomputador", "¿Tiene computador?"),
            new Atributo("tieneAutomovil", "fami_tieneautomovil", "¿Tiene automóvil?"),
            new Atributo("tieneLavadora", "fami_tienelavadora", "¿Tiene lavadora?"),
            new Atributo("cuartosHogar", "fami_cuartoshogar", "Cuartos del hogar"),
            new Atributo("personasHogar", "fami_personashogar", "Personas del hogar"),
            new Atributo("naturalezaColegio", "cole_naturaleza", "Naturaleza del colegio"),
            new Atributo("areaColegio", "cole_area_ubicacion", "Área del colegio"),
            new Atributo("jornada", "cole_jornada", "Jornada"),
            new Atributo("caracterColegio", "cole_caracter", "Carácter del colegio"),
            new Atributo("calendarioColegio", "cole_calendario", "Calendario del colegio"),
            new Atributo("deptoColegio", "cole_depto_ubicacion", "Departamento del colegio"));

    private CatalogoAtributos() {
    }

    public static Optional<Atributo> porClave(String clave) {
        return TODOS.stream().filter(a -> a.clave().equals(clave)).findFirst();
    }

    /** Lista de columnas para el SELECT: los predictores y, al final, la clase. */
    static String columnasSql() {
        return TODOS.stream().map(Atributo::columna).collect(Collectors.joining(", "))
                + ", " + COLUMNA_CLASE;
    }
}
