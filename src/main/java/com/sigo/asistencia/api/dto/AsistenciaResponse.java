package com.sigo.asistencia.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AsistenciaResponse(

        Long id,

        Long plazaId,
        String plaza,

        Long turnoId,
        String turno,

        Long controladorId,
        String controlador,

        LocalDate fecha,

        Integer programados,
        Integer presentes,
        Integer ausentes,

        /*
         * Cantidad de personal adicional
         * solicitado para apoyar el turno.
         *
         * No interviene en el porcentaje
         * de asistencia.
         */
        Integer apoyoSolicitado,

        /*
         * Descripción del apoyo solicitado.
         */
        String detalleApoyo,

        BigDecimal porcentaje,

        String notas,

        List<AusenciaResponse> ausencias,

        List<EvidenciaResponse> evidencias

) {
}