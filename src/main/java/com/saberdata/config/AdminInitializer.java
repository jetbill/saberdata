package com.saberdata.config;

import com.saberdata.model.Rol;
import com.saberdata.model.Usuario;
import com.saberdata.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Al arrancar crea el usuario ADMIN inicial (desde variables de entorno) si todavía no existe. */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AdminProperties props;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(AdminProperties props, UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.props = props;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.password() == null || props.password().isBlank()) {
            log.warn("ADMIN_PASSWORD no está definido: no se crea el usuario administrador inicial.");
            return;
        }
        if (usuarios.existsByUsername(props.username())) {
            return;
        }
        usuarios.save(new Usuario(props.username(), props.email().toLowerCase(),
                passwordEncoder.encode(props.password()), Rol.ADMIN));
        log.info("Usuario administrador inicial '{}' creado.", props.username());
    }
}
