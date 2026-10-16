package com.saberdata.dto;

import java.util.UUID;

/** Respuesta completa del asistente a un mensaje del usuario. */
public record RespuestaChatResponse(UUID conversacionId, String respuesta) {
}
