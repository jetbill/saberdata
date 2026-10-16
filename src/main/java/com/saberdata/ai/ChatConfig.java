package com.saberdata.ai;

import com.saberdata.repository.ConversacionRepository;
import com.saberdata.repository.MensajeRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configura el agente: modelo (Ollama), instrucciones, memoria persistente y herramientas. */
@Configuration
public class ChatConfig {

    static final String SYSTEM_PROMPT = """
            Eres SaberData, un asistente que analiza los resultados de las pruebas Saber 11
            (ICFES, Colombia, periodo 2022-4).

            Reglas:
            - Responde SIEMPRE en español, de forma clara y breve.
            - Solo hablas de los resultados Saber 11 y de los factores asociados al rendimiento
              (estrato, jornada, naturaleza del colegio, departamento, acceso a internet y computador).
            - Si la pregunta no tiene que ver con ese tema, explica amablemente que solo puedes ayudar con Saber 11.
            - Para cualquier cifra o porcentaje DEBES usar las herramientas disponibles. Nunca inventes datos.
            - Si las herramientas indican que no hay datos cargados, díselo al usuario.
            - El rendimiento se clasifica como ALTO o BAJO.
            """;

    /** Memoria del chat guardada en PostgreSQL con ventana de los últimos N mensajes. */
    @Bean
    ChatMemory chatMemory(ConversacionRepository conversaciones, MensajeRepository mensajes,
                          @Value("${app.chat.memory-window}") int ventana) {
        return new PostgresChatMemory(conversaciones, mensajes, ventana);
    }

    /** Cliente de chat único: el builder ya viene configurado con Ollama por Spring AI. */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory, EstadisticasTools tools) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(tools)
                .build();
    }
}
