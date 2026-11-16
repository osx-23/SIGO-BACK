package com.sigo.asistencia.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record PermutaRequest(
        @NotNull @Positive Integer codigoSolicitante,
        @NotNull LocalDate fechaSolicitante,
        @NotNull @Positive Long turnoSolicitanteId,
        @NotNull @Positive Long plazaSolicitanteId,
        @NotNull @Positive Integer codigoAceptante,
        @NotNull LocalDate fechaAceptante,
        @NotNull @Positive Long turnoAceptanteId,
        @NotNull @Positive Long plazaAceptanteId
) {}
