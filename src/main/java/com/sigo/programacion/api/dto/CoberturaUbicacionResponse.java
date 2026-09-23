package com.sigo.programacion.api.dto;

import java.time.LocalDate;
import java.util.Map;

public record CoberturaUbicacionResponse(
        Long ubicacionId,
        String codigo,
        String nombre,
        Map<LocalDate, Long> porDia
) {
}
