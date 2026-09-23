package com.sigo.programacion.api.dto;

import java.time.LocalDate;

public record ProgramacionDiaResponse(
        Long programacionId,
        Long trabajadorId,
        Integer codigoTrabajador,
        String nombreTrabajador,
        Long plazaId,
        String plazaCodigo,
        LocalDate fecha,
        String estado
) {
}
