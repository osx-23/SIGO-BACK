package com.sigo.security.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AviLoginRequest(
        @NotNull
        @Positive
        Integer codigo
) {
}
