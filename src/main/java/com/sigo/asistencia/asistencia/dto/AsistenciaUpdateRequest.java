package com.sigo.asistencia.asistencia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record AsistenciaUpdateRequest(

        @NotNull(message = "La plaza es obligatoria")
        Long plazaId,

        @NotNull(message = "El turno es obligatorio")
        Long turnoId,

        @NotNull(message = "El controlador es obligatorio")
        Long controladorId,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotNull(message = "La cantidad de programados es obligatoria")
        @Min(
                value = 0,
                message = "La cantidad de programados no puede ser negativa"
        )
        Integer programados,

        @NotNull(message = "La cantidad de presentes es obligatoria")
        @Min(
                value = 0,
                message = "La cantidad de presentes no puede ser negativa"
        )
        Integer presentes,

        /*
         * Cantidad de personal adicional
         * solicitado como apoyo para el turno.
         *
         * No interviene en el cálculo
         * del porcentaje de asistencia.
         */
        @Min(
                value = 0,
                message = "El apoyo solicitado no puede ser negativo"
        )
        Integer apoyoSolicitado,

        /*
         * Descripción opcional del apoyo solicitado.
         *
         * Ejemplo:
         * "Se solicitó apoyo de un agente del turno B."
         */
        @Size(
                max = 500,
                message = "El detalle del apoyo no puede superar los 500 caracteres"
        )
        String detalleApoyo,

        String notas,

        @Valid
        List<AusenciaRequest> ausencias

) {
}