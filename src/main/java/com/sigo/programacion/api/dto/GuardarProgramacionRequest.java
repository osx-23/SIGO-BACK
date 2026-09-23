package com.sigo.programacion.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record GuardarProgramacionRequest(
        @NotNull Long plazaId,
        @NotEmpty List<@Valid TurnoItemRequest> programaciones
) {

    public record TurnoItemRequest(
            @NotNull Long trabajadorId,
            @NotNull LocalDate fecha,
            @NotBlank String estado
    ) {
    }
}
