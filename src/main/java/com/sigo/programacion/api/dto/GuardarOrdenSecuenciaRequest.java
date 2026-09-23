package com.sigo.programacion.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GuardarOrdenSecuenciaRequest(

        @NotNull
        Long plazaId,

        @NotBlank
        String grupo,

        @NotEmpty
        List<@Valid OrdenAgenteRequest> agentes

) {

    public record OrdenAgenteRequest(

            @NotNull
            Long agenteId,

            @NotNull
            Integer orden

    ) {
    }
}
