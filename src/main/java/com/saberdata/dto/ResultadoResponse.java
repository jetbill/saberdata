package com.saberdata.dto;

import com.saberdata.model.Rendimiento;
import com.saberdata.model.ResultadoSaber11;

/** Fila del dataset expuesta por la API. */
public record ResultadoResponse(
        Long id, String genero, String deptoResidencia, String estrato,
        String educacionMadre, String educacionPadre,
        String tieneInternet, String tieneComputador, String tieneAutomovil, String tieneLavadora,
        String cuartosHogar, String personasHogar,
        String naturalezaColegio, String areaColegio, String jornada, String caracterColegio,
        String calendarioColegio, String deptoColegio, Rendimiento rendimiento) {

    public static ResultadoResponse desde(ResultadoSaber11 r) {
        return new ResultadoResponse(r.getId(), r.getEstuGenero(), r.getEstuDeptoReside(),
                r.getFamiEstratovivienda(), r.getFamiEducacionmadre(), r.getFamiEducacionpadre(),
                r.getFamiTieneinternet(), r.getFamiTienecomputador(), r.getFamiTieneautomovil(),
                r.getFamiTienelavadora(), r.getFamiCuartoshogar(), r.getFamiPersonashogar(),
                r.getColeNaturaleza(), r.getColeAreaUbicacion(), r.getColeJornada(), r.getColeCaracter(),
                r.getColeCalendario(), r.getColeDeptoUbicacion(), r.getRendimiento());
    }
}
