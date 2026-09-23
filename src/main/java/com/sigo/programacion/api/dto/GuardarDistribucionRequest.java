package com.sigo.programacion.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GuardarDistribucionRequest(
        @NotNull Long plazaId,
        @NotEmpty List<@Valid DistribucionItemRequest> distribuciones
) {

    public record DistribucionItemRequest(
            @NotNull Long programacionTurnoId,
            @NotNull Long ubicacionId,
            String observacion
    ) {
    }
}
