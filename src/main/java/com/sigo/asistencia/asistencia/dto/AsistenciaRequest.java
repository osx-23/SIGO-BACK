package com.sigo.asistencia.asistencia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record AsistenciaRequest(

        @NotNull
        Long plazaId,

        @NotNull
        Long turnoId,

        @NotNull
        Long controladorId,

        @NotNull
        LocalDate fecha,

        @NotNull
        @Min(1)
        Integer programados,

        @NotNull
        @Min(0)
        Integer presentes,

        /*
         * Cantidad de personal adicional
         * solicitado como apoyo.
         *
         * No se considera para calcular
         * el porcentaje de asistencia.
         */
        @Min(
                value = 0,
                message = "El apoyo solicitado no puede ser negativo"
        )
        Integer apoyoSolicitado,

        /*
         * Descripción opcional del apoyo.
         *
         * Ejemplo:
         * "Se solicitó apoyo de un agente de P3."
         */
        @Size(
                max = 500,
                message = "El detalle del apoyo no puede superar los 500 caracteres"
        )
        String detalleApoyo,

        String notas,

        @Valid
        List<AusenciaRequest> ausencias,

        List<@NotBlank String> evidencias

) {
}