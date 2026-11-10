package com.sigo.security.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(
        @NotBlank @Size(min = 8, max = 72) String passwordNueva,
        boolean exigirCambioAlIngresar
) {}
