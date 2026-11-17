package com.sigo.asistencia.api.dto;

import jakarta.validation.constraints.NotNull;

public record AusenciaRequest(
        @NotNull Long trabajadorId,
        @NotNull Long motivoId,
        String observacion,
        Boolean excepcionPlaza
) {
    public AusenciaRequest(Long trabajadorId, Long motivoId, String observacion) {
        this(trabajadorId, motivoId, observacion, false);
    }
}
