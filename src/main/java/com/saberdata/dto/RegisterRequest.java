package com.saberdata.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para registrar un usuario nuevo (siempre con rol USER). */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "solo letras, números, punto, guion y guion bajo")
        String username,

        @NotBlank @Email @Size(max = 150)
        String email,

        // BCrypt solo usa los primeros 72 bytes, por eso el máximo.
        @NotBlank @Size(min = 8, max = 72)
        String password) {
}
