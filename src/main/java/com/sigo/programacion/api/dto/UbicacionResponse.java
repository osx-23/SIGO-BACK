package com.sigo.programacion.api.dto;

public record UbicacionResponse(
        Long id,
        Long plazaId,
        String codigo,
        String nombre,
        String tipo,
        Long viaId,
        Boolean activo,
        Integer orden
) {
}
