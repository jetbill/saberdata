# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

SaberData Colombia: API REST (Java 21, Spring Boot 4.1, Spring Web MVC, Spring Security 7 con JWT, Spring Data JPA, Flyway, PostgreSQL 16) que analiza resultados Saber 11 (ICFES 2022-4) y expone un chatbot basado en Spring AI + Ollama. Documentación en español; código, DTOs y mensajes de error también están en español. Más detalle en `README.md`, `docs/arquitectura.md` y `docs/modelo-datos.md`.

## Comandos

```bash
docker compose up -d          # PostgreSQL local (usa docker-compose.yaml, credenciales de .env)
./gradlew bootRun             # arranca la API en :8090 (requiere variables de entorno cargadas, ver abajo)
./gradlew build               # compila + tests
./gradlew test                # todos los tests (requiere Docker: Testcontainers)
./gradlew test --tests com.saberdata.security.SeguridadWebMvcTest   # una clase de test
./gradlew test --tests '*EstadisticasRepositoryTest.nombreDelMetodo' # un método
./gradlew importarDatos       # carga data/saber11_20224_preparado.csv en resultado_saber11 (-Preemplazar=true para vaciar antes)
```

- `application.yaml` **casi no tiene valores por defecto** (la única excepción es `CHAT_MEMORY_WINDOW`, que cae en `20`): las propiedades vienen de variables de entorno (`PORT`, `DB_*`, `JWT_SECRET`, `OLLAMA_*`, `ADMIN_*`, `CORS_ALLOWED_ORIGINS`, `APPLICATION_NAME`, etc.). Los valores viven en `.env`; no se añaden valores por defecto al yaml. Copia `.env.example` a `.env` y cárgalo en el shell antes de `bootRun` (PowerShell: ver `README.md`; bash: `set -a; source .env; set +a`). Los valores por defecto que lista el README están en `.env.example`.
- `JWT_SECRET` obligatorio (≥32 caracteres). Si `ADMIN_PASSWORD` no está vacío, `AdminInitializer` crea el usuario ADMIN inicial.
- El chat necesita Ollama corriendo con un modelo que soporte tools (`ollama pull llama3.2`).

## Arquitectura

Capas MVC bajo `com.saberdata`: `controller` → `service` → `repository` (JPA) → PostgreSQL; `dto` (records, separados de las entidades `model`); `exception` (`ApiException` + `GlobalExceptionHandler`, todos los errores salen con el mismo JSON: `timestamp, status, error, mensaje, ruta, detalles`); `config`/`security` para seguridad. El chatbot vive aparte en el paquete `ai`.

- **Esquema**: lo gestiona Flyway (`src/main/resources/db/migration/V1__esquema.sql`); Hibernate solo valida (`ddl-auto: validate`). Cambios de esquema = nueva migración `V<n>__*.sql`, y la entidad debe coincidir o el arranque falla.
- **Seguridad**: stateless, `oauth2ResourceServer(jwt)` con JWT HS256 propio (`JwtService`); el claim `rol` se mapea a `ROLE_USER`/`ROLE_ADMIN`. Públicos: `/api/auth/register`, `/api/auth/login`, `/actuator/health`; `/api/admin/**` solo ADMIN; el resto requiere token. 401/403 en JSON vía `security/Json*Handler`.
- **CAPTCHA**: `AuthController` llama a `CaptchaService.verificar(captchaToken)` antes de `login` y `registrar`; valida el token de Cloudflare Turnstile contra su endpoint siteverify con `CAPTCHA_SECRET`. `CAPTCHA_ENABLED=false` lo desactiva (los tests lo traen desactivado; `SeguridadWebMvcTest` mockea el servicio). Si el proveedor no responde, 503. El token es de un solo uso: por eso el registro no hace login automático y el front lleva al usuario a `/login`.
- **Chatbot (`ai`)**: `ChatService` valida que la conversación sea del usuario y delega en un `ChatClient` configurado en `ChatConfig` con (a) `PostgresChatMemory`, que persiste todo el historial en `mensaje` pero entrega al modelo solo los últimos `app.chat.memory-window` mensajes, y (b) `EstadisticasTools` (métodos `@Tool` con consultas agregadas) para que el LLM no invente cifras. Hay endpoint normal y de streaming SSE (`.../mensajes/stream`); si Ollama no responde se devuelve 503. Protocolo del stream (`ChatController`): eventos sin nombre = trozos de texto, `event: fin` = respuesta completa, `event: error` = falló a mitad; un stream sin `fin` ni `error` es una conexión cortada. Los errores previos al primer trozo salen como JSON normal con su código HTTP. Si cambias este protocolo, actualiza `chatApi.ts` del frontend.
- **Importación (`importacion`)**: `ImportadorSaber11` carga el CSV por lotes con `JdbcTemplate`; se ejecuta con el perfil `importar` (`ImportadorRunner`) mediante la tarea Gradle `importarDatos`, que lee `.env` por su cuenta. Se niega a cargar si la tabla ya tiene filas salvo `-Preemplazar=true`. El CSV (`data/*.csv`) no se versiona.
- **Minería de datos (`mineria`)**: `MineriaService` entrena con Weka un árbol **J48** (variable objetivo `rendimiento`, 17 atributos nominales de `CatalogoAtributos`), lo evalúa con validación cruzada (`MINERIA_PLIEGUES`), lo guarda serializado en `MINERIA_MODELO_RUTA` y lo carga al arrancar. Endpoints: `GET /api/mineria/{modelo,atributos,arbol}`, `POST /api/mineria/prediccion` y `POST /api/admin/mineria/entrenar` (solo ADMIN). Sin modelo, `atributos`/`arbol`/`prediccion` devuelven 503. El modelo se lee con un `ObjectInputStream` propio (DevTools carga las clases del proyecto con otro cargador) y nunca debe cargarse desde un archivo subido por usuarios. Weka es GPL v3. `MineriaServiceTest` prueba todo con datos sintéticos, sin base de datos.
- **Estadísticas/resultados**: consultas JPQL agregadas y `Specifications` en `ResultadoSaber11Repository`; la variable objetivo es `Rendimiento` (ALTO/BAJO).

## Tests

Usan Testcontainers con PostgreSQL (`EstadisticasRepositoryTest`, `SaberdataApplicationTests`, `ImportadorSaber11Test`), MockMvc para seguridad (`SeguridadWebMvcTest`) y pruebas sin base de datos (`CaptchaServiceTest`, `MineriaServiceTest`); Docker debe estar activo. Los tests no leen `.env`: usan `src/test/resources/application.properties` con sus propios valores.

## Frontend

El cliente React vive en un repositorio aparte, `../saberdata-front` (Vite, React, TypeScript, Material UI). Corre en `http://localhost:5173`, el único origen que permite `CORS_ALLOWED_ORIGINS`, y consume esta API en `:8090`. Los DTO están replicados a mano en `saberdata-front/src/api/tipos.ts`: si cambias un DTO, actualiza ese archivo.

## Git

Ramas: `master` y `develop` (trabajo en curso).
