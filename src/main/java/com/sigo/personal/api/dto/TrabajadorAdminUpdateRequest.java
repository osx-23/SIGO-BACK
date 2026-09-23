package com.sigo.personal.api.dto;

import jakarta.validation.constraints.NotNull;

public record TrabajadorAdminUpdateRequest(
        @NotNull Long plazaId,
        @NotNull Boolean activo
) {}
