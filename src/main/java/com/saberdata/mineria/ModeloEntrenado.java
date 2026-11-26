package com.saberdata.mineria;

import weka.classifiers.trees.J48;
import weka.core.Instances;

import java.io.Serializable;
import java.time.Instant;

/**
 * Lo que se guarda en disco: el clasificador, la estructura de datos con la que se entrenó (nombres de atributos y
 * valores posibles, sin filas), las métricas de la evaluación y el árbol en texto.
 */
public record ModeloEntrenado(J48 clasificador, Instances cabecera, Metricas metricas, String arbol,
                              Instant entrenadoEn) implements Serializable {
}
