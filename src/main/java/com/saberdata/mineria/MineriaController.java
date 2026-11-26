package com.saberdata.mineria;

import com.saberdata.dto.ArbolResponse;
import com.saberdata.dto.AtributoResponse;
import com.saberdata.dto.ModeloResponse;
import com.saberdata.dto.PrediccionRequest;
import com.saberdata.dto.PrediccionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Minería de datos para cualquier usuario con sesión: estado del modelo, atributos y predicción. */
@RestController
@RequestMapping("/api/mineria")
public class MineriaController {

    private final MineriaService servicio;

    public MineriaController(MineriaService servicio) {
        this.servicio = servicio;
    }

    /** Métricas del modelo; responde 200 con {@code disponible=false} si todavía no se entrenó. */
    @GetMapping("/modelo")
    public ModeloResponse modelo() {
        return servicio.estado();
    }

    /** Atributos y valores que conoce el modelo (503 si todavía no se entrenó). */
    @GetMapping("/atributos")
    public List<AtributoResponse> atributos() {
        return servicio.atributos();
    }

    @GetMapping("/arbol")
    public ArbolResponse arbol() {
        return servicio.arbol();
    }

    @PostMapping("/prediccion")
    public PrediccionResponse predecir(@Valid @RequestBody PrediccionRequest req) {
        return servicio.predecir(req.atributos());
    }
}
