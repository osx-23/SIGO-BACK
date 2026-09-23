package com.sigo.programacion.api.dto;

import java.time.LocalDate;
import java.util.List;

public record MiHorarioResponse(
        Long trabajadorId,
        Integer codigo,
        String nombre,
        Long plazaId,
        String plazaCodigo,
        String lider,
        List<HorarioDiaResponse> dias
) {

    public record HorarioDiaResponse(
            LocalDate fecha,
            String estado,
            String ubicacionCodigo,
            String ubicacionNombre
    ) {
    }
}
