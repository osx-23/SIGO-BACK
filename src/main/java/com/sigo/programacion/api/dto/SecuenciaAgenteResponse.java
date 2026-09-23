package com.sigo.programacion.api.dto;

public record SecuenciaAgenteResponse(
        Long id,
        Long agenteId,
        Integer codigo,
        String nombre,
        Long plazaId,
        String plazaCodigo,
        String grupo,
        Integer orden
) {
}
