package com.sigo.avi.api.dto;

import com.sigo.avi.domain.AviAccion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record AviRegistroUpdateRequest(
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
