package com.saberdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * Propiedades del JWT ({@code app.jwt.*}). El secreto viene de la variable de entorno
 * JWT_SECRET y nunca se guarda en el repositorio.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes) {

    public JwtProperties {
        // HS256 exige una clave de al menos 256 bits (32 bytes).
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "Falta JWT_SECRET o es demasiado corto: se requieren al menos 32 caracteres.");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("JWT_EXPIRATION_MINUTES debe ser mayor que 0.");
        }
    }
}
