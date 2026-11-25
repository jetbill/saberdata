package com.saberdata.service;

import com.saberdata.config.CaptchaProperties;
import com.saberdata.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

/**
 * Verifica el token de Cloudflare Turnstile que genera el widget del frontend.
 * Sin esta verificación en el servidor el CAPTCHA no protege nada: cualquiera puede llamar a la API directamente.
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    private final CaptchaProperties props;
    private final RestClient http;

    @Autowired
    public CaptchaService(CaptchaProperties props, RestClient.Builder builder) {
        this(props, builder.requestFactory(conTiempoLimite()).build());
    }

    /** Para pruebas: permite inyectar un cliente HTTP simulado. */
    CaptchaService(CaptchaProperties props, RestClient http) {
        this.props = props;
        this.http = http;
    }

    private static SimpleClientHttpRequestFactory conTiempoLimite() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        return fabrica;
    }

    /** No hace nada si el CAPTCHA está desactivado; si no, lanza {@link ApiException} cuando el token no es válido. */
    public void verificar(String token) {
        if (!props.enabled()) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Completa la verificación CAPTCHA.");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", props.secret());
        form.add("response", token);

        Map<?, ?> respuesta;
        try {
            respuesta = http.post()
                    .uri(props.verifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException ex) {
            log.warn("No se pudo contactar con Turnstile: {}", ex.toString());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo validar el CAPTCHA. Inténtalo de nuevo en unos minutos.");
        }

        if (respuesta == null || !Boolean.TRUE.equals(respuesta.get("success"))) {
            // El token es de un solo uso y caduca a los pocos minutos.
            log.debug("CAPTCHA rechazado: {}", respuesta == null ? null : respuesta.get("error-codes"));
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La verificación CAPTCHA no es válida o expiró. Inténtalo de nuevo.");
        }
    }
}
