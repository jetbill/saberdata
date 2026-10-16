package com.saberdata.security;

import com.saberdata.config.JwtProperties;
import com.saberdata.config.SecurityConfig;
import com.saberdata.controller.AdminController;
import com.saberdata.controller.AuthController;
import com.saberdata.controller.EstadisticasController;
import com.saberdata.dto.ResumenResponse;
import com.saberdata.dto.TokenResponse;
import com.saberdata.model.Rol;
import com.saberdata.model.Usuario;
import com.saberdata.service.AuthService;
import com.saberdata.service.EstadisticasService;
import com.saberdata.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Evidencia de control de acceso: prueba la cadena real de Spring Security (JWT firmado y
 * validado de verdad) sin base de datos; los servicios están simulados con Mockito.
 */
@WebMvcTest(controllers = {AuthController.class, AdminController.class, EstadisticasController.class})
@EnableConfigurationProperties(JwtProperties.class)
@Import({SecurityConfig.class, JwtService.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=secreto-solo-para-pruebas-con-mas-de-32-caracteres",
        "app.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:5173"
})
class SeguridadWebMvcTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwtService;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    EstadisticasService estadisticasService;

    private String bearer(Rol rol) {
        Usuario u = new Usuario("tester", "tester@correo.com", "hash", rol);
        return "Bearer " + jwtService.generar(u).accessToken();
    }

    @Test
    void sinToken_devuelve401EnJson() throws Exception {
        mvc.perform(get("/api/estadisticas/resumen"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenInvalido_devuelve401() throws Exception {
        mvc.perform(get("/api/estadisticas/resumen").header("Authorization", "Bearer no.es.un.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioNormalEnRutaAdmin_devuelve403EnJson() throws Exception {
        mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer(Rol.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminEnRutaAdmin_devuelve200() throws Exception {
        when(authService.listar()).thenReturn(List.of());
        mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer(Rol.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioAutenticadoAccedeAEstadisticas_devuelve200() throws Exception {
        when(estadisticasService.resumen()).thenReturn(new ResumenResponse(0, 0, 0, 0.0, 0.0));
        mvc.perform(get("/api/estadisticas/resumen").header("Authorization", bearer(Rol.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEstudiantes").value(0));
    }

    @Test
    void meSinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginEsPublico() throws Exception {
        when(authService.login(any())).thenReturn(TokenResponse.bearer("token-de-prueba", 3600));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"clave12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-de-prueba"));
    }

    @Test
    void registroConDatosInvalidos_devuelve400ConDetalles() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\",\"email\":\"no-es-correo\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isNotEmpty());
    }

    @Test
    void cors_permiteElOrigenDelFrontend() throws Exception {
        mvc.perform(options("/api/estadisticas/resumen")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
