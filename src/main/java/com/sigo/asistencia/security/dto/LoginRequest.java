package com.sigo.asistencia.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull Integer codigo,
        @NotBlank String password
) {}
