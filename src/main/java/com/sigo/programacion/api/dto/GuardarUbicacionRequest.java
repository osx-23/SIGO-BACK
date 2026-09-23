package com.sigo.programacion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GuardarUbicacionRequest(
        @NotNull Long plazaId,
        @NotBlank String codigo,
        @NotBlank String nombre,
        @NotBlank String tipo,
        Integer orden
) {
}
