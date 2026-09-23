package com.sigo.asistencia.inventario.dto.response;

public record InventarioUsuarioResponse(
        Long trabajadorId,
        Integer codigo,
        String nombre,
        String rol,
        Long plazaId,
        String plaza
) {
}