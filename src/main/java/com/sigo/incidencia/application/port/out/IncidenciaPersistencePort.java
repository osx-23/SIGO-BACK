package com.sigo.incidencia.application.port.out;

import com.sigo.incidencia.domain.EstadoIncidencia;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidenciaPersistencePort {

    boolean plazaActiva(Long plazaId);

    boolean turnoExiste(Long turnoId);

    boolean trabajadorActivo(Long trabajadorId);

    boolean tipoActivo(Long tipoId);

    Optional<Long> plazaIdDeViaActiva(Long viaId);

    List<TipoData> listarTiposActivos();

    IncidenciaData crear(
            Long plazaId,
            Long turnoId,
            Long registradoPorId,
            Long tipoId,
            Long viaId,
            LocalDate fecha,
            LocalTime hora,
            String descripcion
    );

    List<IncidenciaData> listar(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId
    );

    Optional<IncidenciaData> obtener(Long id);

    IncidenciaData actualizar(
            Long id,
            Long plazaId,
            Long turnoId,
            Long tipoId,
            Long viaId,
            LocalDate fecha,
            LocalTime hora,
            String descripcion
    );

    IncidenciaData marcarAtendido(
            Long id,
            OffsetDateTime fechaAtendido
    );

    long contarPorEstado(EstadoIncidencia estado);

    long contarEvidencias(Long incidenciaId);

    EvidenciaData guardarEvidencia(
            Long incidenciaId,
            String urlArchivo,
            String publicId
    );

    Optional<EvidenciaData> obtenerEvidencia(
            Long incidenciaId,
            Long evidenciaId
    );

    EvidenciaData actualizarEvidencia(
            Long incidenciaId,
            Long evidenciaId,
            String urlArchivo,
            String publicId
    );

    Optional<String> eliminarEvidencia(
            Long incidenciaId,
            Long evidenciaId
    );

    record TipoData(Long id, String nombre) {
    }

    record EvidenciaData(
            Long id,
            String urlArchivo,
            String publicId,
            String tipo,
            OffsetDateTime fechaCreacion
    ) {
    }

    record IncidenciaData(
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
            List<EvidenciaData> evidencias
    ) {
    }
}
