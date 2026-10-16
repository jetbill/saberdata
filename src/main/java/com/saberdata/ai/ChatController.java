package com.saberdata.ai;

import com.saberdata.dto.ConversacionResponse;
import com.saberdata.dto.CrearConversacionRequest;
import com.saberdata.dto.EnviarMensajeRequest;
import com.saberdata.dto.MensajeResponse;
import com.saberdata.dto.RespuestaChatResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

/** API del chatbot. El usuario se toma del token JWT (nunca del cuerpo de la petición). */
@RestController
@RequestMapping("/api/chat/conversaciones")
public class ChatController {

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

    /** Respuesta por trozos con Server-Sent Events (el frontend la muestra mientras se genera). */
    @PostMapping(value = "/{id}/mensajes/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> enviarEnStreaming(@PathVariable UUID id,
                                          @Valid @RequestBody EnviarMensajeRequest req,
                                          Authentication auth) {
        return chatService.responderEnStreaming(auth.getName(), id, req.contenido());
    }
}
