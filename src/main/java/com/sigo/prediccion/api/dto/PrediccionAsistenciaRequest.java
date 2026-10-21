package com.sigo.prediccion.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record PrediccionAsistenciaRequest(
        @NotNull @Positive Long plazaId,
        @NotNull @Positive Long turnoId,
        @NotNull LocalDate fecha,
        @NotNull @Min(0) Integer programados
) {
}
