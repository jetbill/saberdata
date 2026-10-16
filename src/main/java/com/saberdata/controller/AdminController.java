package com.saberdata.controller;

import com.saberdata.dto.UsuarioResponse;
import com.saberdata.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints solo para ADMIN. La regla se aplica dos veces (defensa en profundidad):
 * en la cadena de filtros ({@code /api/admin/**}) y con {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> usuarios() {
        return authService.listar();
    }
}
