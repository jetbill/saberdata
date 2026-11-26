package com.saberdata.mineria;

import com.saberdata.dto.ModeloResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entrenamiento del modelo, solo para ADMIN. Como en {@code AdminController}, la regla se aplica dos veces:
 * en la cadena de filtros ({@code /api/admin/**}) y con {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/api/admin/mineria")
@PreAuthorize("hasRole('ADMIN')")
public class MineriaAdminController {

    private final MineriaService servicio;

    public MineriaAdminController(MineriaService servicio) {
        this.servicio = servicio;
    }

    /** Entrena y evalúa un modelo nuevo con todos los datos. Puede tardar; la respuesta trae las métricas. */
    @PostMapping("/entrenar")
    public ModeloResponse entrenar() {
        return servicio.entrenar();
    }
}
