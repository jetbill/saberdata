package com.saberdata.repository;

import com.saberdata.model.Conversacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversacionRepository extends JpaRepository<Conversacion, UUID> {

    /** Conversaciones de un usuario, la más reciente primero. */
    List<Conversacion> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId);

    /** Garantiza que un usuario solo accede a sus propias conversaciones. */
    Optional<Conversacion> findByIdAndUsuarioId(UUID id, Long usuarioId);
}
