# Arquitectura

Aplicación MVC por capas con Spring Web MVC (servlet). El frontend (React, otro proyecto) consume una API REST JSON.

## Capas

```mermaid
flowchart TD
    FE["Frontend React (SPA)"] -->|"HTTP + JWT"| SEC["Spring Security<br/>CORS, JWT, roles"]
    SEC --> CTRL["controller / ai.ChatController<br/>@RestController (solo HTTP y DTO)"]
    CTRL --> SVC["service / ai.ChatService<br/>lógica de negocio"]
    SVC --> REPO["repository<br/>Spring Data JPA"]
    REPO --> DB[("PostgreSQL 16<br/>Flyway")]
    SVC -.-> AI["ai.ChatConfig<br/>ChatClient + memoria + tools"]
    AI -.-> OLL["Ollama (LLM local)"]
    EXC["exception.GlobalExceptionHandler<br/>errores JSON uniformes"] -.-> CTRL
```

| Paquete | Responsabilidad |
|---|---|
| `config` | Seguridad (`SecurityConfig`), propiedades (`JwtProperties`, `AdminProperties`), admin inicial. |
| `security` | Respuestas 401/403 en JSON. |
| `controller` | Endpoints REST de autenticación, administración, estadísticas y resultados. |
| `service` | Reglas de negocio: autenticación, JWT, estadísticas, listado de resultados. |
| `repository` | Acceso a datos (consultas JPQL agregadas, Specifications). |
| `model` | Entidades JPA y enumerados. |
| `dto` | Records de entrada y salida (separados de las entidades). |
| `exception` | `ApiException` y `@RestControllerAdvice`. |
| `ai` | Chatbot: `ChatConfig`, `ChatService`, `ChatController`, `PostgresChatMemory`, `EstadisticasTools`. |

## Seguridad

* Stateless: no hay sesión ni cookies; cada petición lleva `Authorization: Bearer <JWT>`.
* El login valida la contraseña (BCrypt) y emite un JWT firmado HS256 con `sub` = usuario y claim `rol`.
* `oauth2ResourceServer(jwt)` valida firma y expiración; el claim `rol` se convierte en `ROLE_USER` / `ROLE_ADMIN`.
* Reglas: `POST /api/auth/register` y `/login` y `/actuator/health` públicos; `/api/admin/**` solo ADMIN
  (además `@PreAuthorize`); todo lo demás requiere token.

## Flujo del chat

```mermaid
sequenceDiagram
    actor U as Usuario
    participant C as ChatController
    participant S as ChatService
    participant CC as ChatClient (Spring AI)
    participant M as PostgresChatMemory
    participant O as Ollama (llama3.2)
    participant T as EstadisticasTools
    participant DB as PostgreSQL

    U->>C: POST /api/chat/conversaciones/{id}/mensajes (JWT)
    C->>S: responder(usuario, id, texto)
    S->>DB: ¿la conversación es de este usuario?
    S->>CC: prompt().user(texto) con conversationId
    CC->>M: get(id): últimos N mensajes
    M->>DB: SELECT mensajes recientes
    CC->>O: system prompt + historial + pregunta + definición de tools
    O-->>CC: "llama a rendimientoPorEstrato()"
    CC->>T: ejecuta la herramienta
    T->>DB: consulta agregada (GROUP BY)
    DB-->>T: conteos reales
    T-->>CC: texto con porcentajes
    CC->>O: resultado de la herramienta
    O-->>CC: respuesta final en español
    CC->>M: add(id, pregunta y respuesta)
    M->>DB: INSERT mensajes
    CC-->>S: respuesta
    S-->>U: 200 {conversacionId, respuesta}
```

* **Memoria:** `PostgresChatMemory` guarda todo el historial pero solo entrega al modelo los últimos
  `app.chat.memory-window` mensajes (20 por defecto).
* **Tool calling:** el modelo no inventa cifras; invoca métodos `@Tool` que consultan la base.
* **Streaming:** `POST .../mensajes/stream` devuelve la misma respuesta por trozos con Server-Sent Events.
* **Errores:** si Ollama no responde, la API devuelve `503` en JSON con un mensaje claro.

## Diagramas interactivos

Versiones HTML (se abren en el navegador, con tema claro/oscuro):

* [Arquitectura de la aplicación](arquitectura-diagrama.html)
* [Flujo del chat (diagrama de secuencia)](flujo-chat-diagrama.html)
