package com.saberdata.dto;

import com.saberdata.model.Rol;
import com.saberdata.model.Usuario;

import java.time.Instant;

/** Vista pública de un usuario (nunca incluye el hash de la contraseña). */
public record UsuarioResponse(Long id, String username, String email, Rol rol, boolean activo, Instant creadoEn) {

    public static UsuarioResponse desde(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRol(), u.isActivo(), u.getCreadoEn());
    }
}
