package com.saberdata.service;

import com.saberdata.config.JwtProperties;
import com.saberdata.dto.TokenResponse;
import com.saberdata.model.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Genera los JWT firmados (HS256) que se entregan en el login. */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public JwtService(JwtEncoder encoder, JwtProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public TokenResponse generar(Usuario usuario) {
        Duration duracion = Duration.ofMinutes(props.expirationMinutes());
        Instant ahora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("saberdata")
                .subject(usuario.getUsername())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(duracion))
                .claim("rol", usuario.getRol().name())   // se mapea a ROLE_USER / ROLE_ADMIN
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return TokenResponse.bearer(token, duracion.toSeconds());
    }
}
