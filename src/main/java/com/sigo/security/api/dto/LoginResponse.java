package com.sigo.security.api.dto;

import java.util.List;

public record LoginResponse(
        String token,
        String tipo,
        long expiresIn,
        UsuarioSesion usuario
) {
    public record UsuarioSesion(
            Long id,
            Long trabajadorId,
            Integer codigo,
            String nombre,
            String rol,
            Long plazaId,
            String plaza,
            List<String> modulos,
            Boolean requiereCambioPassword
    ) {}
}
