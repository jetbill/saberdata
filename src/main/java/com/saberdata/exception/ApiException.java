package com.saberdata.exception;

import org.springframework.http.HttpStatus;

/** Error de negocio con el código HTTP que debe devolver la API (409, 404, 401, 503...). */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
