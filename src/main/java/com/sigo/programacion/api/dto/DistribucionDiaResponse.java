package com.sigo.programacion.api.dto;

import java.time.LocalDate;

public record DistribucionDiaResponse(
        Long distribucionId,
        Long programacionTurnoId,
        Long trabajadorId,
        Integer codigoTrabajador,
        String nombreTrabajador,
        LocalDate fecha,
        String estado,
        Long ubicacionId,
        String ubicacionCodigo,
        String ubicacionNombre,
        String ubicacionTipo,
        String observacion
) {
}
