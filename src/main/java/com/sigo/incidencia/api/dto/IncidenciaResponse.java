package com.sigo.incidencia.api.dto;

import com.sigo.incidencia.domain.EstadoIncidencia;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public record IncidenciaResponse(
        Long id,
        Long plazaId,
        String plazaCodigo,
        Long turnoId,
        String turnoCodigo,
        Long registradoPorId,
        Integer registradoPorCodigo,
        String registradoPorNombre,
        Long tipoId,
        String tipoNombre,
        Long viaId,
        Integer viaNumero,
        String viaNombre,
        LocalDate fecha,
        LocalTime hora,
        String descripcion,
        EstadoIncidencia estado,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaAtendido,
        List<Evidencia> evidencias
) {
    public record Evidencia(
            Long id,
            String urlArchivo,
            String publicId,
            String tipo,
            OffsetDateTime fechaCreacion
    ) {
    }
}
