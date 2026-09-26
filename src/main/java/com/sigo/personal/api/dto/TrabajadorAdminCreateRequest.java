package com.sigo.personal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TrabajadorAdminCreateRequest(
        @NotNull
        @Positive
        Integer codigo,

        @NotBlank
        @Size(max = 150)
        String nombreCompleto,

        @NotNull
        Long puestoId,

        @NotNull
        Long plazaId,

        @NotBlank
        @Size(min = 5, max = 72)
        String passwordInicial
) {
}
