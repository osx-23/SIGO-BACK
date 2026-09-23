package com.sigo.asistencia.security.dto;

import java.util.List;

public record MeResponse(
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
