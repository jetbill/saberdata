package com.saberdata.repository;

import com.saberdata.model.Mensaje;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface MensajeRepository extends JpaRepository<Mensaje, Long> {

    List<Mensaje> findByConversacionIdOrderByIdAsc(UUID conversacionId);

    /** Últimos mensajes (más nuevo primero); sirve para la ventana de memoria del chat. */
    List<Mensaje> findByConversacionIdOrderByIdDesc(UUID conversacionId, Pageable pageable);

    /** Borra todos los mensajes de una conversación (las consultas derivadas de borrado necesitan transacción). */
    @Transactional
    void deleteByConversacionId(UUID conversacionId);
}
