package com.saberdata.mineria;

import java.io.Serializable;

/**
 * Resultado de evaluar el modelo con validación cruzada. Se guarda junto al modelo y se devuelve tal cual por la API.
 *
 * @param exactitudBase      exactitud de predecir siempre la clase mayoritaria (referencia mínima que debe superar el modelo)
 * @param matrizConfusion    filas = clase real, columnas = clase predicha, en el orden ALTO, BAJO
 * @param hojas              hojas del árbol final entrenado con todos los datos
 * @param tamanoArbol        nodos del árbol final
 * @param segundos           duración del entrenamiento y la evaluación
 */
public record Metricas(
        int instancias, int atributos, int pliegues, int minObj,
        double exactitud, double exactitudBase, double kappa, double auc,
        ClaseMetricas alto, ClaseMetricas bajo,
        long[][] matrizConfusion,
        int hojas, int tamanoArbol, long segundos) implements Serializable {

    public record ClaseMetricas(double precision, double recall, double f1) implements Serializable {
    }
}
