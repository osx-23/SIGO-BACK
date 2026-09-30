package com.sigo.asistencia.api.dto;

public record TopAusenciaResponse(
        Long trabajadorId,
        Integer codigo,
        String nombre,
        Long totalAusencias
) {
}
