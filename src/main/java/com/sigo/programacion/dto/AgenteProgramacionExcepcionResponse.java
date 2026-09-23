package com.sigo.programacion.dto;

import java.time.OffsetDateTime;

public record AgenteProgramacionExcepcionResponse(
        Long id,
        Long trabajadorId,
        Integer codigoTrabajador,
        String nombreTrabajador,
        Long plazaId,
        String plazaCodigo,
        Boolean permiteA,
        Boolean permiteB,
        Boolean permiteC,
        String motivo,
        String color,
        Boolean activo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
