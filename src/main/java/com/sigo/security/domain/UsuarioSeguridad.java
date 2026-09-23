package com.sigo.security.domain;

public record UsuarioSeguridad(
        Long id,
        Integer codigo,
        String nombre,
        RolSeguridad rol,
        Long plazaId,
        String plazaCodigo,
        String passwordHash,
        Boolean requiereCambioPassword,
        Boolean activo
) {
    public boolean estaActivo() {
        return Boolean.TRUE.equals(activo);
    }
}
