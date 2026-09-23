package com.sigo.personal.api.dto;

public record PlazaCatalogoResponse(
        Long id,
        String codigo,
        String descripcion,
        Boolean activo
) {
}
