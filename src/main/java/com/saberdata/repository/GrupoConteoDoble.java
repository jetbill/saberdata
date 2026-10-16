package com.saberdata.repository;

/** Igual que {@link GrupoConteo} pero agrupando por dos columnas (internet y computador). */
public record GrupoConteoDoble(String primero, String segundo, Long total, Long alto) {
}
