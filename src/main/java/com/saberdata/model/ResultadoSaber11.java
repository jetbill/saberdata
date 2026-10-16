package com.saberdata.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un estudiante del dataset Saber 11 (ICFES, periodo 2022-4).
 * Todas las columnas descriptivas son texto, ya normalizado (ASCII, sin tildes).
 */
@Entity
@Table(name = "resultado_saber11")
public class ResultadoSaber11 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estu_genero")
    private String estuGenero;

    @Column(name = "estu_depto_reside")
    private String estuDeptoReside;

    @Column(name = "fami_estratovivienda")
    private String famiEstratovivienda;

    @Column(name = "fami_educacionmadre")
    private String famiEducacionmadre;

    @Column(name = "fami_educacionpadre")
    private String famiEducacionpadre;

    @Column(name = "fami_tieneinternet")
    private String famiTieneinternet;

    @Column(name = "fami_tienecomputador")
    private String famiTienecomputador;

    @Column(name = "fami_tieneautomovil")
    private String famiTieneautomovil;

    @Column(name = "fami_tienelavadora")
    private String famiTienelavadora;

    @Column(name = "fami_cuartoshogar")
    private String famiCuartoshogar;

    @Column(name = "fami_personashogar")
    private String famiPersonashogar;

    @Column(name = "cole_naturaleza")
    private String coleNaturaleza;

    @Column(name = "cole_area_ubicacion")
    private String coleAreaUbicacion;

    @Column(name = "cole_jornada")
    private String coleJornada;

    @Column(name = "cole_caracter")
    private String coleCaracter;

    @Column(name = "cole_calendario")
    private String coleCalendario;

    @Column(name = "cole_depto_ubicacion")
    private String coleDeptoUbicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "rendimiento")
    private Rendimiento rendimiento;

    public ResultadoSaber11() {
        // Requerido por JPA
    }

    public Long getId() { return id; }
    public String getEstuGenero() { return estuGenero; }
    public void setEstuGenero(String estuGenero) { this.estuGenero = estuGenero; }
    public String getEstuDeptoReside() { return estuDeptoReside; }
    public void setEstuDeptoReside(String estuDeptoReside) { this.estuDeptoReside = estuDeptoReside; }
    public String getFamiEstratovivienda() { return famiEstratovivienda; }
    public void setFamiEstratovivienda(String famiEstratovivienda) { this.famiEstratovivienda = famiEstratovivienda; }
    public String getFamiEducacionmadre() { return famiEducacionmadre; }
    public void setFamiEducacionmadre(String famiEducacionmadre) { this.famiEducacionmadre = famiEducacionmadre; }
    public String getFamiEducacionpadre() { return famiEducacionpadre; }
    public void setFamiEducacionpadre(String famiEducacionpadre) { this.famiEducacionpadre = famiEducacionpadre; }
    public String getFamiTieneinternet() { return famiTieneinternet; }
    public void setFamiTieneinternet(String famiTieneinternet) { this.famiTieneinternet = famiTieneinternet; }
    public String getFamiTienecomputador() { return famiTienecomputador; }
    public void setFamiTienecomputador(String famiTienecomputador) { this.famiTienecomputador = famiTienecomputador; }
    public String getFamiTieneautomovil() { return famiTieneautomovil; }
    public void setFamiTieneautomovil(String famiTieneautomovil) { this.famiTieneautomovil = famiTieneautomovil; }
    public String getFamiTienelavadora() { return famiTienelavadora; }
    public void setFamiTienelavadora(String famiTienelavadora) { this.famiTienelavadora = famiTienelavadora; }
    public String getFamiCuartoshogar() { return famiCuartoshogar; }
    public void setFamiCuartoshogar(String famiCuartoshogar) { this.famiCuartoshogar = famiCuartoshogar; }
    public String getFamiPersonashogar() { return famiPersonashogar; }
    public void setFamiPersonashogar(String famiPersonashogar) { this.famiPersonashogar = famiPersonashogar; }
    public String getColeNaturaleza() { return coleNaturaleza; }
    public void setColeNaturaleza(String coleNaturaleza) { this.coleNaturaleza = coleNaturaleza; }
    public String getColeAreaUbicacion() { return coleAreaUbicacion; }
    public void setColeAreaUbicacion(String coleAreaUbicacion) { this.coleAreaUbicacion = coleAreaUbicacion; }
    public String getColeJornada() { return coleJornada; }
    public void setColeJornada(String coleJornada) { this.coleJornada = coleJornada; }
    public String getColeCaracter() { return coleCaracter; }
    public void setColeCaracter(String coleCaracter) { this.coleCaracter = coleCaracter; }
    public String getColeCalendario() { return coleCalendario; }
    public void setColeCalendario(String coleCalendario) { this.coleCalendario = coleCalendario; }
    public String getColeDeptoUbicacion() { return coleDeptoUbicacion; }
    public void setColeDeptoUbicacion(String coleDeptoUbicacion) { this.coleDeptoUbicacion = coleDeptoUbicacion; }
    public Rendimiento getRendimiento() { return rendimiento; }
    public void setRendimiento(Rendimiento rendimiento) { this.rendimiento = rendimiento; }
}
