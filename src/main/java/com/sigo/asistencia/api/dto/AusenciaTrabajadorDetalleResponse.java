package com.sigo.asistencia.api.dto;

import java.time.LocalDate;

public record AusenciaTrabajadorDetalleResponse(
        Long trabajadorId,
        Integer codigo,
        String nombre,
        LocalDate fecha,
        String motivo,
        String observacion,
        String plaza,
        String turno
) {
}
