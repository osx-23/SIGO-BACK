package com.sigo.programacion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AsignarSecuenciaRequest(
        @NotNull Long agenteId,
        @NotNull Long plazaId,
        @NotBlank String grupo
) {
}
