package com.sigo.incidencia.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record IncidenciaRequest(
        @NotNull Long plazaId,
        @NotNull Long turnoId,
        @NotNull Long tipoId,
        Long viaId,
        @NotNull LocalDate fecha,
        @NotNull LocalTime hora,
        @NotBlank @Size(max = 2000) String descripcion
) {
}
