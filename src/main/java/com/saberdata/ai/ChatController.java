package com.saberdata.ai;

import com.saberdata.dto.ConversacionResponse;
import com.saberdata.dto.CrearConversacionRequest;
import com.saberdata.dto.EnviarMensajeRequest;
import com.saberdata.dto.MensajeResponse;
import com.saberdata.dto.RespuestaChatResponse;
import com.saberdata.exception.ApiException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** API del chatbot. El usuario se toma del token JWT (nunca del cuerpo de la petición). */
@RestController
@RequestMapping("/api/chat/conversaciones")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversacionResponse crear(@Valid @RequestBody(required = false) CrearConversacionRequest req,
                                      Authentication auth) {
        return chatService.crear(auth.getName(), req == null ? null : req.titulo());
    }

    @GetMapping
    public List<ConversacionResponse> listar(Authentication auth) {
        return chatService.listar(auth.getName());
    }

    @GetMapping("/{id}/mensajes")
    public List<MensajeResponse> historial(@PathVariable UUID id, Authentication auth) {
        return chatService.historial(auth.getName(), id);
    }

    /** Respuesta completa (espera a que el modelo termine). */
    @PostMapping("/{id}/mensajes")
    public RespuestaChatResponse enviar(@PathVariable UUID id,
                                        @Valid @RequestBody EnviarMensajeRequest req,
                                        Authentication auth) {
        return chatService.responder(auth.getName(), id, req.contenido());
    }

    /**
     * Respuesta por trozos con Server-Sent Events (el frontend la muestra mientras se genera).
     * Protocolo: eventos sin nombre = trozos de texto; {@code event: fin} = respuesta completa;
     * {@code event: error} = falló a mitad (el dato es el mensaje). Un stream que se cierra sin
     * {@code fin} ni {@code error} es una conexión cortada y el cliente debe tratarlo como fallo.
     * Los errores anteriores al primer trozo (404, 503…) siguen saliendo como JSON con su código HTTP.
     */
    @PostMapping(value = "/{id}/mensajes/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> enviarEnStreaming(@PathVariable UUID id,
                                                           @Valid @RequestBody EnviarMensajeRequest req,
                                                           Authentication auth) {
        Flux<String> trozos = chatService.responderEnStreaming(auth.getName(), id, req.contenido());
        return Flux.defer(() -> {
            AtomicBoolean empezo = new AtomicBoolean();
            return trozos
                    .doOnNext(t -> empezo.set(true))
                    .map(t -> ServerSentEvent.builder(t).build())
                    .concatWith(Flux.just(ServerSentEvent.<String>builder("ok").event("fin").build()))
                    .onErrorResume(ex -> {
                        if (!empezo.get()) {
                            return Flux.error(ex); // aún no hay respuesta HTTP: lo resuelve GlobalExceptionHandler
                        }
                        log.warn("Streaming del chat interrumpido: {}", ex.toString());
                        String mensaje = ex instanceof ApiException ? ex.getMessage()
                                : "La respuesta se interrumpió. Inténtalo de nuevo.";
                        return Flux.just(ServerSentEvent.<String>builder(mensaje).event("error").build());
                    });
        });
    }
}
