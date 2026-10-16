package com.saberdata.dto;

/** Respuesta del login: el JWT, su tipo y los segundos que dura. */
public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {

    public static TokenResponse bearer(String accessToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
