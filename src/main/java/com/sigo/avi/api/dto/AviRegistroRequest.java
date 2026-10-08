package com.sigo.avi.api.dto;

import com.sigo.avi.domain.AviAccion;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AviRegistroRequest(
        @NotNull
        UUID id,

        @NotBlank
        @Size(max = 40)
        String placa,

        @NotNull
        @Positive
        Integer via,

        @NotNull
        AviAccion accion,

        @NotNull
        OffsetDateTime fechaHoraEvento,

        @Size(max = 2000)
        String textoReconocido
) {
}
