package com.sigo.programacion.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GrupoLiderRequest(
        @NotNull Long agenteId,
        @NotNull Long controladorId,
        @NotNull Long plazaId,
        LocalDate fechaInicio
) {
}
