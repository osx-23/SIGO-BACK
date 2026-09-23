package com.sigo.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String passwordActual,
        @NotBlank @Size(min = 8, max = 72) String passwordNueva
) {}
