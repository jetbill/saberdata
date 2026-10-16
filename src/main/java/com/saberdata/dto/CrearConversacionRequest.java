package com.saberdata.dto;

import jakarta.validation.constraints.Size;

/** El título es opcional; si falta se usa uno por defecto y luego el inicio de la primera pregunta. */
public record CrearConversacionRequest(@Size(max = 200) String titulo) {
}
