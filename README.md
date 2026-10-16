# SaberData Colombia — Backend

API REST que analiza los resultados de las pruebas **Saber 11** (ICFES, periodo 2022-4) y ofrece un
**chatbot inteligente** que responde con datos reales de la base. Proyecto de aula de *Desarrollo Web Avanzado*.

**Stack:** Java 21 · Spring Boot 4.1 (Spring Web MVC) · Spring Security 7 (JWT) · Spring Data JPA ·
Flyway · PostgreSQL 16 · Spring AI + Ollama (LLM local).

Documentación: [Modelo de datos y diccionario](docs/modelo-datos.md) · [Arquitectura y flujo del chat](docs/arquitectura.md)

## Requisitos

* **Java 21**
* **Docker** (PostgreSQL local y pruebas con Testcontainers)
* **Ollama** con un modelo que soporte *tools*:
  ```bash
  ollama pull llama3.2
  ```

## Cómo arrancar

1. Copia `.env.example` a `.env` y cambia los valores (el archivo `.env` está en `.gitignore`).
2. Levanta PostgreSQL:
   ```bash
   docker compose up -d
   ```
3. Define las variables de entorno. En PowerShell (carga el `.env`):
   ```powershell
   Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object { $k,$v = $_ -split '=',2; Set-Item "env:$k" $v }
   ```
   En Linux/macOS: `set -a; source .env; set +a`
4. Arranca la aplicación (Ollama debe estar en ejecución para el chat):
   ```bash
   ./gradlew bootRun
   ```
   La API queda en `http://localhost:8080`. Flyway crea las tablas al arrancar.

> `JWT_SECRET` es obligatorio (mínimo 32 caracteres). Si `ADMIN_PASSWORD` está definido, se crea un usuario
> ADMIN inicial con `ADMIN_USERNAME` / `ADMIN_EMAIL`.


## Variables de entorno

| Variable | Por defecto | Descripción |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `saberdata` | Conexión a PostgreSQL. |
| `DB_USER` / `DB_PASSWORD` | `saberdata` / `saberdata` | Credenciales (también las usa `compose.yaml`). |
| `JWT_SECRET` | — (obligatorio) | Secreto HS256, mínimo 32 caracteres. |
| `JWT_EXPIRATION_MINUTES` | `60` | Duración del token. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes permitidos (separados por coma). |
| `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin` / `admin@saberdata.local` / vacío | Usuario ADMIN inicial. |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | URL de Ollama. |
| `OLLAMA_MODEL` | `llama3.2` | Modelo del chat. |
| `CHAT_MEMORY_WINDOW` | `20` | Mensajes recientes que se envían al modelo. |

## Endpoints

| Método y ruta | Acceso | Descripción |
|---|---|---|
| `POST /api/auth/register` | Público | Crea un usuario (rol USER). |
| `POST /api/auth/login` | Público | Devuelve el JWT. |
| `GET /api/auth/me` | Autenticado | Usuario del token. |
| `GET /api/admin/usuarios` | Solo ADMIN | Lista de usuarios. |
| `GET /api/estadisticas/resumen` | Autenticado | Total y % ALTO/BAJO. |
| `GET /api/estadisticas/rendimiento-por-{estrato,jornada,naturaleza,departamento,internet-computador}` | Autenticado | Rendimiento agrupado, listo para graficar. |
| `GET /api/resultados` | Autenticado | Paginado; filtros `estrato`, `jornada`, `naturaleza`, `departamento`, `rendimiento`, `page`, `size`. |
| `POST /api/chat/conversaciones` | Autenticado | Crea una conversación. |
| `GET /api/chat/conversaciones` | Autenticado | Lista las del usuario. |
| `GET /api/chat/conversaciones/{id}/mensajes` | Autenticado | Historial. |
| `POST /api/chat/conversaciones/{id}/mensajes` | Autenticado | Pregunta; respuesta completa. |
| `POST /api/chat/conversaciones/{id}/mensajes/stream` | Autenticado | Pregunta; respuesta por SSE. |
| `GET /actuator/health` | Público | Estado de la aplicación. |

Los errores siempre tienen el mismo formato JSON: `timestamp`, `status`, `error`, `mensaje`, `ruta`, `detalles`.

## Ejemplos con curl

```bash
# Registro y login
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"username":"ana","email":"ana@correo.com","password":"clave12345"}'

TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"ana","password":"clave12345"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

# Estadísticas
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/estadisticas/resumen
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/estadisticas/rendimiento-por-estrato
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/resultados?estrato=Estrato%201&rendimiento=ALTO&page=0&size=20"

# Chat
curl -X POST http://localhost:8080/api/chat/conversaciones -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"titulo":"Estratos"}'
# Copia el "id" devuelto y úsalo en lugar de ID:
curl -X POST http://localhost:8080/api/chat/conversaciones/ID/mensajes -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"contenido":"¿Cómo rinden los estudiantes de estrato 1?"}'
# Streaming (-N desactiva el búfer de curl)
curl -N -X POST http://localhost:8080/api/chat/conversaciones/ID/mensajes/stream -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"contenido":"Compara las jornadas"}'
```

## Pruebas

```bash
./gradlew test
```

* `SeguridadWebMvcTest`: control de acceso con MockMvc (401 sin token, 403 de USER en `/api/admin/**`, 200 con token, CORS).
* `EstadisticasRepositoryTest` y `SaberdataApplicationTests`: usan **Testcontainers** con PostgreSQL 16 (necesitan Docker;
  si Docker no está activo se omiten). Validan Flyway, el mapeo JPA, las consultas agregadas y el flujo completo.

## Seguridad y secretos

Ninguna contraseña ni secreto está en el repositorio: todo viene de variables de entorno. `.env` está en
`.gitignore`; solo se versiona `.env.example`.
