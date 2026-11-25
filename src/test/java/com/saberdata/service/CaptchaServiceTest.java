package com.saberdata.service;

import com.saberdata.config.CaptchaProperties;
import com.saberdata.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Sin red: el cliente HTTP está simulado y se comprueba qué se envía a Turnstile y cómo se interpreta la respuesta. */
class CaptchaServiceTest {

    private static final String URL = "https://captcha.test/siteverify";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();

    private CaptchaService servicio(boolean activo) {
        return new CaptchaService(new CaptchaProperties(activo, activo ? "clave-secreta" : "", URL), builder.build());
    }

    private static void esperaEstado(Runnable accion, HttpStatus esperado) {
        assertThatThrownBy(accion::run)
                .isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(esperado));
    }

    @Test
    void desactivado_noExigeTokenNiLlamaAlProveedor() {
        assertThatCode(() -> servicio(false).verificar(null)).doesNotThrowAnyException();
        servidor.verify(); // no se esperaba ninguna petición
    }

    @Test
    void activado_sinToken_responde400() {
        esperaEstado(() -> servicio(true).verificar("  "), HttpStatus.BAD_REQUEST);
        servidor.verify();
    }

    @Test
    void tokenValido_enviaSecretoYTokenComoFormulario() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formDataContains(Map.of("secret", "clave-secreta", "response", "token-ok")))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> servicio(true).verificar("token-ok")).doesNotThrowAnyException();
        servidor.verify();
    }

    @Test
    void tokenRechazado_responde400() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess("{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}",
                        MediaType.APPLICATION_JSON));

        esperaEstado(() -> servicio(true).verificar("token-malo"), HttpStatus.BAD_REQUEST);
    }

    @Test
    void proveedorCaido_responde503() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        esperaEstado(() -> servicio(true).verificar("token"), HttpStatus.SERVICE_UNAVAILABLE);
    }
}
