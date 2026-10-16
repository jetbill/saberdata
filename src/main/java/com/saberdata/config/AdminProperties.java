package com.saberdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Datos del usuario ADMIN inicial ({@code app.admin.*}), tomados de variables de entorno. */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String username, String email, String password) {
}
