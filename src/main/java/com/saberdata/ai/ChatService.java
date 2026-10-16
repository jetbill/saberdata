package com.saberdata.ai;

import com.saberdata.dto.ConversacionResponse;
import com.saberdata.dto.MensajeResponse;
import com.saberdata.dto.RespuestaChatResponse;
import com.saberdata.exception.ApiException;
import com.saberdata.model.Conversacion;
import com.saberdata.model.Usuario;
import com.saberdata.repository.ConversacionRepository;
import com.saberdata.repository.MensajeRepository;
import com.saberdata.repository.UsuarioRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

/**
 * Gestiona las conversaciones del chatbot. Cada usuario solo accede a las suyas.
 * El historial lo guarda {@link PostgresChatMemory} a través del advisor de memoria de Spring AI,
 * por eso aquí no se insertan mensajes a mano. No es @Transactional a propósito:
 * la llamada al LLM puede tardar y no debe mantener una transacción abierta.
 */
@Service
public class ChatService {

    static final String TITULO_POR_DEFECTO = "Nueva conversación";

    private final ChatClient chatClient;
    private final ConversacionRepository conversaciones;
    private final MensajeRepository mensajes;
    private final UsuarioRepository usuarios;

    public ChatService(ChatClient chatClient, ConversacionRepository conversaciones,
                       MensajeRepository mensajes, UsuarioRepository usuarios) {
        this.chatClient = chatClient;
        this.conversaciones = conversaciones;
        this.mensajes = mensajes;
        this.usuarios = usuarios;
    }

    public ConversacionResponse crear(String username, String titulo) {
        Usuario usuario = buscarUsuario(username);
        String t = (titulo == null || titulo.isBlank()) ? TITULO_POR_DEFECTO : titulo.trim();
        return ConversacionResponse.desde(conversaciones.save(new Conversacion(usuario, t)));
    }

    public List<ConversacionResponse> listar(String username) {
        Usuario usuario = buscarUsuario(username);
        return conversaciones.findByUsuarioIdOrderByCreadaEnDesc(usuario.getId()).stream()
                .map(ConversacionResponse::desde).toList();
    }

    public List<MensajeResponse> historial(String username, UUID conversacionId) {
        Conversacion c = buscarConversacion(username, conversacionId);
        return mensajes.findByConversacionIdOrderByIdAsc(c.getId()).stream()
                .map(MensajeResponse::desde).toList();
    }

    /** Envía la pregunta al modelo y devuelve la respuesta completa. */
    public RespuestaChatResponse responder(String username, UUID conversacionId, String texto) {
        Conversacion c = buscarConversacion(username, conversacionId);
        ponerTituloSiHaceFalta(c, texto);
        try {
            String respuesta = chatClient.prompt()
                    .user(texto)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, c.getId().toString()))
                    .call()
                    .content();
            return new RespuestaChatResponse(c.getId(), respuesta);
        } catch (RuntimeException ex) {
            throw traducir(ex);
        }
    }

    /** Igual que {@link #responder}, pero entrega la respuesta por trozos (SSE). */
    public Flux<String> responderEnStreaming(String username, UUID conversacionId, String texto) {
        Conversacion c = buscarConversacion(username, conversacionId);
        ponerTituloSiHaceFalta(c, texto);
        return chatClient.prompt()
                .user(texto)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, c.getId().toString()))
                .stream()
                .content()
                .onErrorMap(RuntimeException.class, this::traducir);
    }

    // ---------------------------------------------------------------- auxiliares

    private Usuario buscarUsuario(String username) {
        return usuarios.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));
    }

    /** Busca la conversación SOLO si pertenece al usuario; si no, responde 404 (no revela que existe). */
    private Conversacion buscarConversacion(String username, UUID id) {
        Usuario usuario = buscarUsuario(username);
        return conversaciones.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conversación no encontrada."));
    }

    /** Si la conversación aún tiene el título por defecto, usa el inicio de la primera pregunta. */
    private void ponerTituloSiHaceFalta(Conversacion c, String texto) {
        if (TITULO_POR_DEFECTO.equals(c.getTitulo())) {
            String t = texto.strip();
            c.setTitulo(t.length() > 60 ? t.substring(0, 60) + "…" : t);
            conversaciones.save(c);
        }
    }

    /** Convierte errores de conexión con Ollama en un 503 claro; el resto se propaga igual. */
    private RuntimeException traducir(RuntimeException ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String tipo = t.getClass().getName();
            String msg = String.valueOf(t.getMessage()).toLowerCase();
            if (t instanceof java.net.ConnectException
                    || t instanceof java.net.UnknownHostException
                    || t instanceof java.net.SocketTimeoutException
                    || tipo.endsWith("ResourceAccessException")
                    || tipo.endsWith("WebClientRequestException")) {
                return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "El servicio de IA (Ollama) no está disponible. Verifica que Ollama esté en ejecución.");
            }
            if (tipo.endsWith("NonTransientAiException") && msg.contains("not found")) {
                return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "El modelo de IA no está instalado en Ollama. Ejecuta: ollama pull llama3.2");
            }
        }
        return ex;
    }
}
