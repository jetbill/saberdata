package com.saberdata.service;

import com.saberdata.dto.LoginRequest;
import com.saberdata.dto.RegisterRequest;
import com.saberdata.dto.TokenResponse;
import com.saberdata.dto.UsuarioResponse;
import com.saberdata.exception.ApiException;
import com.saberdata.model.Rol;
import com.saberdata.model.Usuario;
import com.saberdata.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Lógica de registro, login y consulta de usuarios. */
@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Registro público: siempre crea un usuario con rol USER. */
    @Transactional
    public UsuarioResponse registrar(RegisterRequest req) {
        String username = req.username().trim();
        String email = req.email().trim().toLowerCase();
        if (usuarios.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "El nombre de usuario ya está en uso.");
        }
        if (usuarios.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado.");
        }
        Usuario nuevo = new Usuario(username, email, passwordEncoder.encode(req.password()), Rol.USER);
        return UsuarioResponse.desde(usuarios.save(nuevo));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        // Mismo mensaje para usuario inexistente o clave errada (no revela cuál falló).
        Usuario usuario = usuarios.findByUsername(req.username().trim())
                .filter(Usuario::isActivo)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos."));
        return jwtService.generar(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorUsername(String username) {
        return usuarios.findByUsername(username)
                .map(UsuarioResponse::desde)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarios.findAll().stream().map(UsuarioResponse::desde).toList();
    }
}
