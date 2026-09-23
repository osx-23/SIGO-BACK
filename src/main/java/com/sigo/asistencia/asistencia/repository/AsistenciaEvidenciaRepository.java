package com.sigo.asistencia.asistencia.repository;

import com.sigo.asistencia.asistencia.entity.AsistenciaEvidencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsistenciaEvidenciaRepository
        extends JpaRepository<AsistenciaEvidencia, Long> {

    /*
     * Obtener todas las evidencias
     * de una asistencia.
     */
    List<AsistenciaEvidencia> findByAsistenciaId(
            Long asistenciaId
    );

    /*
     * Obtener evidencias de VARIAS asistencias en una sola consulta.
     * Se usa en listar() para evitar N+1 (una query por registro).
     */
    List<AsistenciaEvidencia> findByAsistenciaIdIn(
            List<Long> asistenciaIds
    );

    /*
     * Obtener una evidencia verificando
     * que realmente pertenece a la asistencia.
     *
     * Se utilizará para eliminar fotografías
     * durante la edición.
     */
    Optional<AsistenciaEvidencia>
    findByIdAndAsistenciaId(
            Long evidenciaId,
            Long asistenciaId
    );
}