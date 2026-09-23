package com.sigo.inventario.application.security;

public record InventarioUsuarioActual(
        Long trabajadorId,
        Integer codigo,
        String nombre,
        Long plazaId,
        String plazaCodigo,
        Long rolId,
        String rolCodigo,
        String rolSistema
) {
    public boolean esSupervisorSistema() {
        return "SUPERVISOR".equalsIgnoreCase(rolSistema);
    }

    public boolean esControladorSistema() {
        return "CONTROLADOR".equalsIgnoreCase(rolSistema);
    }

    public boolean esOperadorSistema() {
        return "OPERADOR".equalsIgnoreCase(rolSistema);
    }
}
