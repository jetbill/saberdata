package com.saberdata.dto;

import com.saberdata.model.Mensaje;
import com.saberdata.model.RolMensaje;

import java.time.Instant;

public record MensajeResponse(Long id, RolMensaje rol, String contenido, Instant creadoEn) {

    public static MensajeResponse desde(Mensaje m) {
        return new MensajeResponse(m.getId(), m.getRol(), m.getContenido(), m.getCreadoEn());
    }
}
