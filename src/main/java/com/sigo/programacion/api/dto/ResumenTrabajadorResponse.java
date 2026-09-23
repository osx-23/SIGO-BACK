package com.sigo.programacion.api.dto;

import java.util.List;

public record ResumenTrabajadorResponse(
        Long trabajadorId,
        Integer codigo,
        String nombre,
        List<ResumenUbicacionResponse> ubicaciones
) {

    public record ResumenUbicacionResponse(
            String codigo,
            String nombre,
            long veces
    ) {
    }
}
