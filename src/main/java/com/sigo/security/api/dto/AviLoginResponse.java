package com.sigo.security.api.dto;

public record AviLoginResponse(
        String token,
        String tipo,
        long expiresIn,
        Usuario usuario
) {
    public record Usuario(
            Long id,
            Integer codigo,
            String nombre,
            String rol,
            Long plazaId,
            String plaza
    ) {
    }
}
