package com.sigo.personal.api.dto;

public record TrabajadorResponse(
        Long id,
        Integer codigo,
        String nombreCompleto,
        PuestoResumen puesto,
        PlazaResumen plaza,
        String rolSistema,
        Boolean requiereCambioPassword,
        Boolean activo
) {
    public record PuestoResumen(
            Long id,
            String nombre
    ) {
    }

    public record PlazaResumen(
            Long id,
            String codigo,
            String descripcion
    ) {
    }
}
