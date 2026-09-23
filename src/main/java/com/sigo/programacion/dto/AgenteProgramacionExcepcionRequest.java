package com.sigo.programacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AgenteProgramacionExcepcionRequest(
        @NotNull Long trabajadorId,
        @NotNull Long plazaId,
        @NotNull Boolean permiteA,
        @NotNull Boolean permiteB,
        @NotNull Boolean permiteC,
        @Size(max = 500) String motivo,
        @NotBlank
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe tener formato hexadecimal #RRGGBB")
        String color,
        Boolean activo
) {}
