package com.saberdata.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Mensaje de una conversación del chatbot (tabla {@code mensaje}). */
@Entity
@Table(name = "mensaje")
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversacion_id", nullable = false)
    private Conversacion conversacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RolMensaje rol;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    protected Mensaje() {
        // Requerido por JPA
    }

    public Mensaje(Conversacion conversacion, RolMensaje rol, String contenido) {
        this.conversacion = conversacion;
        this.rol = rol;
        this.contenido = contenido;
    }

    public Long getId() { return id; }
    public Conversacion getConversacion() { return conversacion; }
    public RolMensaje getRol() { return rol; }
    public String getContenido() { return contenido; }
    public Instant getCreadoEn() { return creadoEn; }
}
