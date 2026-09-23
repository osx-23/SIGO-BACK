package com.sigo.personal.api.dto;

public record TrabajadorPublicResponse(
        Long id,
        Integer codigo,
        String nombreCompleto,
        PuestoResponse puesto,
        PlazaResponse plaza,
        String rolSistema,
        Boolean requiereCambioPassword,
        Boolean activo
) {
    public record PuestoResponse(
            Long id,
            String nombre
    ) {
    }

    public record PlazaResponse(
            Long id,
            String codigo,
            String descripcion,
            Boolean activo
    ) {
    }
}
