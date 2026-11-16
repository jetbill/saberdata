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
```

- `application.yaml` **no tiene valores por defecto**: todas las propiedades vienen de variables de entorno (`PORT`, `DB_*`, `JWT_SECRET`, `OLLAMA_*`, `ADMIN_*`, `CORS_ALLOWED_ORIGINS`, `CHAT_MEMORY_WINDOW`, `APPLICATION_NAME`, etc.). Copia `.env.example` a `.env` y cárgalo en el shell antes de `bootRun` (PowerShell: ver `README.md`; bash: `set -a; source .env; set +a`). Los valores por defecto que lista el README están en `.env.example`.
- `JWT_SECRET` obligatorio (≥32 caracteres). Si `ADMIN_PASSWORD` no está vacío, `AdminInitializer` crea el usuario ADMIN inicial.
- El chat necesita Ollama corriendo con un modelo que soporte tools (`ollama pull llama3.2`).

## Arquitectura

Capas MVC bajo `com.saberdata`: `controller` → `service` → `repository` (JPA) → PostgreSQL; `dto` (records, separados de las entidades `model`); `exception` (`ApiException` + `GlobalExceptionHandler`, todos los errores salen con el mismo JSON: `timestamp, status, error, mensaje, ruta, detalles`); `config`/`security` para seguridad. El chatbot vive aparte en el paquete `ai`.

- **Esquema**: lo gestiona Flyway (`src/main/resources/db/migration/V1__esquema.sql`); Hibernate solo valida (`ddl-auto: validate`). Cambios de esquema = nueva migración `V<n>__*.sql`, y la entidad debe coincidir o el arranque falla.
- **Seguridad**: stateless, `oauth2ResourceServer(jwt)` con JWT HS256 propio (`JwtService`); el claim `rol` se mapea a `ROLE_USER`/`ROLE_ADMIN`. Públicos: `/api/auth/register`, `/api/auth/login`, `/actuator/health`; `/api/admin/**` solo ADMIN; el resto requiere token. 401/403 en JSON vía `security/Json*Handler`.
- **Chatbot (`ai`)**: `ChatService` valida que la conversación sea del usuario y delega en un `ChatClient` configurado en `ChatConfig` con (a) `PostgresChatMemory`, que persiste todo el historial en `mensaje` pero entrega al modelo solo los últimos `app.chat.memory-window` mensajes, y (b) `EstadisticasTools` (métodos `@Tool` con consultas agregadas) para que el LLM no invente cifras. Hay endpoint normal y de streaming SSE (`.../mensajes/stream`); si Ollama no responde se devuelve 503.
- **Estadísticas/resultados**: consultas JPQL agregadas y `Specifications` en `ResultadoSaber11Repository`; la variable objetivo es `Rendimiento` (ALTO/BAJO).

## Tests

Usan Testcontainers con PostgreSQL (`EstadisticasRepositoryTest`, `SaberdataApplicationTests`) y MockMvc para seguridad (`SeguridadWebMvcTest`); Docker debe estar activo.

## Git

Ramas: `master` y `develop` (trabajo en curso).
