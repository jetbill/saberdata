package com.saberdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensajeRequest(@NotBlank @Size(max = 2000) String contenido) {
}
