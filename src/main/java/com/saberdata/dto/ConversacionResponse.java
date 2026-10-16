package com.saberdata.dto;

import com.saberdata.model.Conversacion;

import java.time.Instant;
import java.util.UUID;

public record ConversacionResponse(UUID id, String titulo, Instant creadaEn) {

    public static ConversacionResponse desde(Conversacion c) {
        return new ConversacionResponse(c.getId(), c.getTitulo(), c.getCreadaEn());
    }
}
