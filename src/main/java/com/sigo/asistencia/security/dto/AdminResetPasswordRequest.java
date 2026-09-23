package com.sigo.asistencia.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(
        @NotBlank @Size(min = 5, max = 72) String passwordNueva,
        boolean exigirCambioAlIngresar
) {}
