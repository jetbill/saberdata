package com.saberdata;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de extremo a extremo con la aplicación completa y PostgreSQL real:
 * arranque (Flyway + validación JPA + JPQL), registro, login, estadísticas con tabla vacía
 * y aislamiento de conversaciones entre usuarios. Se omite si Docker no está activo.
 */
@SpringBootTest(properties = {
        "app.jwt.secret=secreto-solo-para-pruebas-con-mas-de-32-caracteres",
        "app.admin.username=admin",
        "app.admin.email=admin@saberdata.test",
        "app.admin.password=clave-admin-de-pruebas"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SaberdataApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired
    MockMvc mvc;

    @Test
    void contextLoads() {
    }

    @Test
    void flujoCompleto_registroLoginEstadisticasYChat() throws Exception {
        String user = "ana" + UUID.randomUUID().toString().substring(0, 8);
        registrar(user);
        String token = login(user, "clave12345");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user))
                .andExpect(jsonPath("$.rol").value("USER"));

        // Tabla vacía: ceros y listas vacías, sin errores.
        mvc.perform(get("/api/estadisticas/resumen").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEstudiantes").value(0))
                .andExpect(jsonPath("$.porcentajeAlto").value(0.0));
        for (String ruta : new String[]{"estrato", "jornada", "naturaleza", "departamento", "internet-computador"}) {
            mvc.perform(get("/api/estadisticas/rendimiento-por-" + ruta).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
        mvc.perform(get("/api/resultados?estrato=Estrato 1&rendimiento=ALTO").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));

        // Un USER no entra a /api/admin, pero el ADMIN inicial (variables de entorno) sí.
        mvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        String tokenAdmin = login("admin", "clave-admin-de-pruebas");
        mvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        // Conversaciones: cada usuario solo ve las suyas.
        String respuesta = mvc.perform(post("/api/chat/conversaciones").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"titulo\":\"Mi consulta\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String conversacionId = JsonPath.read(respuesta, "$.id");

        mvc.perform(get("/api/chat/conversaciones/" + conversacionId + "/mensajes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/chat/conversaciones/" + conversacionId + "/mensajes")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void registroDuplicado_devuelve409() throws Exception {
        String user = "dup" + UUID.randomUUID().toString().substring(0, 8);
        registrar(user);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoRegistro(user)))
                .andExpect(status().isConflict());
    }

    @Test
    void loginConClaveIncorrecta_devuelve401() throws Exception {
        String user = "bad" + UUID.randomUUID().toString().substring(0, 8);
        registrar(user);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"otra-clave-123\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String cuerpoRegistro(String user) {
        return "{\"username\":\"" + user + "\",\"email\":\"" + user + "@correo.com\",\"password\":\"clave12345\"}";
    }

    private void registrar(String user) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(cuerpoRegistro(user)))
                .andExpect(status().isCreated());
    }

    private String login(String user, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + user + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
