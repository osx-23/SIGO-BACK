package com.sigo.incidencia.application.port.in;

import com.sigo.incidencia.domain.EstadoIncidencia;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public interface IncidenciaUseCase {

    List<Tipo> listarTipos();

    Incidencia registrar(Command command);

    List<Incidencia> listar(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId,
            EstadoIncidencia estado
    );

    Incidencia obtener(Long id);

    Incidencia actualizar(
            Long id,
            Command command
    );

    Incidencia atender(Long id);

    Evidencia subirEvidencia(
            Long incidenciaId,
            byte[] contenido,
            String contentType
    );

    Evidencia reemplazarEvidencia(
            Long incidenciaId,
            Long evidenciaId,
            byte[] contenido,
            String contentType
    );

    void eliminarEvidencia(
            Long incidenciaId,
            Long evidenciaId
    );

    long contarPendientes();

    record Command(
            Long plazaId,
            Long turnoId,
            Long tipoId,
            Long viaId,
            LocalDate fecha,
            LocalTime hora,
            String descripcion
    ) {
    }

    record Tipo(Long id, String nombre) {
    }

    record Evidencia(
            Long id,
            String urlArchivo,
            String publicId,
            String tipo,
            OffsetDateTime fechaCreacion
    ) {
    }

    record Incidencia(
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
    }
}
