package com.sigo.programacion.api.dto;

import java.time.LocalDate;

public record GrupoLiderResponse(
        Long id,
        Long agenteId,
        Integer agenteCodigo,
        String agenteNombre,
        Long controladorId,
        Integer controladorCodigo,
        String controladorNombre,
        Long plazaId,
        String plazaCodigo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Boolean activo
) {
}
