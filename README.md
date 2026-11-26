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
   La API queda en `http://localhost:8090`. Flyway crea las tablas al arrancar.

> `JWT_SECRET` es obligatorio (mínimo 32 caracteres). Si `ADMIN_PASSWORD` está definido, se crea un usuario
> ADMIN inicial con `ADMIN_USERNAME` / `ADMIN_EMAIL`.

## Importar los datos

La tabla `resultado_saber11` se crea vacía y se carga una sola vez desde el CSV del ICFES.

> **El CSV no está en el repositorio** (pesa ~16 MB y `data/*.csv` está en `.gitignore`). Antes de importar,
> copia `saber11_20224_preparado.csv` a la carpeta `data/`. Debe tener encabezado y las 18 columnas de la tabla
> (todas menos `id`), en cualquier orden, separadas por coma y en UTF-8.

Con PostgreSQL en marcha y el `.env` creado (la tarea lo lee sola):

```bash
./gradlew importarDatos
```

* La carga es todo o nada (una transacción). Las filas inválidas se descartan y se listan en el log.
* Si la tabla ya tiene filas, se niega a cargar. Para borrarlas y volver a cargar:
  `./gradlew importarDatos -Preemplazar=true`
* Para verificar: `select count(*) from resultado_saber11;` (con el archivo actual son 106.817 filas).
* Al terminar la aplicación se cierra sola. No interfiere con la API: usa un puerto libre al azar.
* Otro archivo: usa el perfil directamente con
  `./gradlew bootRun --args='--spring.profiles.active=importar --app.importar.archivo=ruta/al/archivo.csv'`
  (en ese caso sí debes tener las variables del `.env` cargadas en la terminal).


## Variables de entorno

| Variable | Por defecto | Descripción |
|---|---|---|
| `PORT` | `8090` | Puerto HTTP de la API. |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `saberdata` | Conexión a PostgreSQL. |
| `DB_USER` / `DB_PASSWORD` | `saberdata` / `saberdata` | Credenciales (también las usa `docker-compose.yaml`). |
| `JWT_SECRET` | — (obligatorio) | Secreto HS256, mínimo 32 caracteres. |
| `JWT_EXPIRATION_MINUTES` | `60` | Duración del token. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes permitidos (separados por coma). |
| `ADMIN_USERNAME` / `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin` / `admin@saberdata.local` / vacío | Usuario ADMIN inicial. |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | URL de Ollama. |
| `OLLAMA_MODEL` | `llama3.2` | Modelo del chat. |
| `CHAT_MEMORY_WINDOW` | `20` | Mensajes recientes que se envían al modelo. |
| `CAPTCHA_ENABLED` | `true` | Exige y verifica el CAPTCHA de Cloudflare Turnstile en login y registro. `false` lo desactiva. |
| `CAPTCHA_SECRET` | `1x0000000000000000000000000000000AA` | Clave **secreta** de Turnstile. El valor por defecto es la de prueba de Cloudflare (siempre aprueba); en producción usa la tuya. |

| `MINERIA_MODELO_RUTA` | `data/modelo-j48.model` | Archivo donde se guarda el modelo de minería de datos (se crea al entrenar; no se versiona). |
| `MINERIA_PLIEGUES` | `10` | Pliegues de la validación cruzada al entrenar. |
| `MINERIA_MIN_OBJ` | `20` | Mínimo de instancias por hoja del árbol J48: más alto, árbol más pequeño. |

El front necesita además la clave pública `VITE_TURNSTILE_SITE_KEY` (ver su README). Con las claves de prueba
(`1x00000000000000000000AA` pública y la secreta de arriba) todo funciona sin cuenta de Cloudflare.

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
| `GET /api/mineria/modelo` | Autenticado | Estado y métricas del modelo J48 (`disponible=false` si aún no se entrenó). |
| `GET /api/mineria/atributos` | Autenticado | Atributos del modelo y sus valores posibles (503 si no está entrenado). |
| `GET /api/mineria/arbol` | Autenticado | Árbol de decisión en texto (máximo 400 líneas). |
| `POST /api/mineria/prediccion` | Autenticado | Predice ALTO/BAJO con probabilidades a partir de `{ "atributos": { "estrato": "Estrato 3", ... } }`. |
| `POST /api/admin/mineria/entrenar` | Solo ADMIN | Entrena y evalúa el modelo con todos los datos (puede tardar). |
| `GET /actuator/health` | Público | Estado de la aplicación. |

Los errores siempre tienen el mismo formato JSON: `timestamp`, `status`, `error`, `mensaje`, `ruta`, `detalles`.

## Ejemplos con curl

```bash
# Registro y login
curl -X POST http://localhost:8090/api/auth/register -H "Content-Type: application/json" \
  -d '{"username":"ana","email":"ana@correo.com","password":"clave12345"}'

TOKEN=$(curl -s -X POST http://localhost:8090/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"ana","password":"clave12345"}' | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

# Estadísticas
curl -H "Authorization: Bearer $TOKEN" http://localhost:8090/api/estadisticas/resumen
curl -H "Authorization: Bearer $TOKEN" http://localhost:8090/api/estadisticas/rendimiento-por-estrato
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8090/api/resultados?estrato=Estrato%201&rendimiento=ALTO&page=0&size=20"

# Chat
curl -X POST http://localhost:8090/api/chat/conversaciones -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"titulo":"Estratos"}'
# Copia el "id" devuelto y úsalo en lugar de ID:
curl -X POST http://localhost:8090/api/chat/conversaciones/ID/mensajes -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"contenido":"¿Cómo rinden los estudiantes de estrato 1?"}'
# Streaming (-N desactiva el búfer de curl)
curl -N -X POST http://localhost:8090/api/chat/conversaciones/ID/mensajes/stream -H "Authorization: Bearer $TOKEN" \
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

## Minería de datos (Weka)

El paquete `mineria` entrena con **Weka** un árbol de decisión **J48** (C4.5) que clasifica el rendimiento (ALTO/BAJO)
a partir de 17 atributos del estudiante, su hogar y su colegio. Flujo:

1. Importa los datos (`./gradlew importarDatos`) y arranca la API.
2. Como ADMIN, llama a `POST /api/admin/mineria/entrenar` (o usa el botón de la pantalla «Predicción» del front).
   Evalúa con validación cruzada, guarda el modelo en `MINERIA_MODELO_RUTA` y lo deja activo; al reiniciar se carga solo.
3. Cualquier usuario con sesión puede consultar `GET /api/mineria/modelo` y predecir con `POST /api/mineria/prediccion`.

Notas: los atributos de la predicción son opcionales (un atributo ausente es un dato faltante, que J48 sabe manejar) y un
valor que el modelo no vio al entrenar se ignora y se avisa en `ignorados`. **Weka se distribuye con licencia GPL v3**.
El archivo del modelo usa serialización de Java: solo cárgalo si lo generó esta aplicación.
