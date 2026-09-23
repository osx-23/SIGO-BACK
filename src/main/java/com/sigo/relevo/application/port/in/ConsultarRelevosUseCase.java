package com.sigo.relevo.application.port.in;

import com.sigo.relevo.domain.EstadoRelevo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public interface ConsultarRelevosUseCase {

    List<Elemento> listarElementos();

    Relevo obtener(Long id);

    List<Relevo> listar(LocalDate inicio, LocalDate fin);

    record Elemento(
            Long id,
            String codigo,
            String nombre,
            String categoria,
            Boolean requiereCantidad,
            Integer orden
    ) {
    }

    record Evidencia(
            Long id,
            String urlArchivo,
            String publicId,
            String tipo,
            OffsetDateTime createdAt
    ) {
    }

    record Checklist(
            Long id,
            Long elementoId,
            String codigo,
            String nombre,
            String categoria,
            EstadoRelevo estado,
            String detalle,
            Integer cantidad,
            List<Evidencia> evidencias
    ) {
    }

    record Via(
            Long id,
            Long viaId,
            Integer numero,
            String nombre,
            EstadoRelevo estado,
            String detalle,
            List<Evidencia> evidencias
    ) {
    }

    record Relevo(
            Long id,
            Long plazaId,
            String plazaCodigo,
            String plazaDescripcion,
            Long turnoId,
            String turnoCodigo,
            String turnoNombre,
            Long operadorId,
            Integer operadorCodigo,
            String operadorNombre,
            LocalDate fecha,
            LocalTime hora,
            String observaciones,
            String resumen,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            List<Checklist> checklist,
            List<Via> vias
    ) {
    }
}
