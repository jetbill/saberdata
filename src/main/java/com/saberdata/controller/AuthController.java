package com.saberdata.controller;

import com.saberdata.dto.LoginRequest;
import com.saberdata.dto.RegisterRequest;
import com.saberdata.dto.TokenResponse;
import com.saberdata.dto.UsuarioResponse;
import com.saberdata.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse register(@Valid @RequestBody RegisterRequest req) {
        return authService.registrar(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /** Usuario dueño del token actual. El nombre de autenticación es el "sub" del JWT. */
    @GetMapping("/me")
    public UsuarioResponse me(Authentication authentication) {
        return authService.obtenerPorUsername(authentication.getName());
    }
}
