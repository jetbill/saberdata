package com.saberdata.ai;

import com.saberdata.model.Conversacion;
import com.saberdata.model.Mensaje;
import com.saberdata.model.RolMensaje;
import com.saberdata.repository.ConversacionRepository;
import com.saberdata.repository.MensajeRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Memoria del chat guardada en PostgreSQL (tablas {@code conversacion} y {@code mensaje}).
 * <p>
 * Guarda TODO el historial (para mostrarlo al usuario), pero al modelo solo le entrega
 * los últimos {@code ventana} mensajes: así el contexto no crece sin límite.
 * El {@code conversationId} de Spring AI es el UUID de la conversación.
 */
public class PostgresChatMemory implements ChatMemory {

    private final ConversacionRepository conversaciones;
    private final MensajeRepository mensajes;
    private final int ventana;

    public PostgresChatMemory(ConversacionRepository conversaciones, MensajeRepository mensajes, int ventana) {
        this.conversaciones = conversaciones;
        this.mensajes = mensajes;
        this.ventana = ventana;
    }

    /** Persiste los mensajes nuevos; solo se guardan los del usuario y del asistente con texto. */
    @Override
    public void add(String conversationId, List<Message> nuevos) {
        Conversacion conversacion = conversaciones.getReferenceById(UUID.fromString(conversationId));
        for (Message m : nuevos) {
            RolMensaje rol = switch (m.getMessageType()) {
                case USER -> RolMensaje.USER;
                case ASSISTANT -> RolMensaje.ASSISTANT;
                default -> null;   // SYSTEM y TOOL no se guardan
            };
            String texto = m.getText();
            if (rol != null && texto != null && !texto.isBlank()) {
                mensajes.save(new Mensaje(conversacion, rol, texto));
            }
        }
    }

    /** Últimos {@code ventana} mensajes, del más antiguo al más reciente. */
    @Override
    public List<Message> get(String conversationId) {
        List<Mensaje> recientes = new ArrayList<>(mensajes.findByConversacionIdOrderByIdDesc(
                UUID.fromString(conversationId), PageRequest.of(0, ventana)));
        Collections.reverse(recientes);
        return recientes.stream().map(this::aMensajeSpringAi).toList();
    }

    @Override
    public void clear(String conversationId) {
        mensajes.deleteByConversacionId(UUID.fromString(conversationId));
    }

    private Message aMensajeSpringAi(Mensaje m) {
        return m.getRol() == RolMensaje.USER
                ? UserMessage.builder().text(m.getContenido()).build()
                : AssistantMessage.builder().content(m.getContenido()).build();
    }
}
