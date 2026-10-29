package com.sigo.programacion.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CrearTrasladoPlazaRequest(
        @NotNull Long trabajadorId,
        @NotNull Long plazaDestinoId,
        @NotNull LocalDate fechaInicio,
        LocalDate fechaFin
) {}
