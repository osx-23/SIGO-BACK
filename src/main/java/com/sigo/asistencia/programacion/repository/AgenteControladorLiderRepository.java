package com.sigo.asistencia.programacion.repository;

import com.sigo.asistencia.programacion.entity.AgenteControladorLider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgenteControladorLiderRepository
        extends JpaRepository<AgenteControladorLider, Long> {

    Optional<AgenteControladorLider>
    findByAgenteIdAndActivoTrue(Long agenteId);

    List<AgenteControladorLider>
    findByControladorIdAndActivoTrue(Long controladorId);

    /*
     * Consulta optimizada para la pantalla de programación.
     *
     * Trae en una sola consulta:
     * - relación líder
     * - agente
     * - controlador
     * - plaza
     *
     * Evita consultas adicionales cuando ProgramacionService
     * construye GrupoLiderResponse.
     */
    @Query("""
        SELECT l
        FROM AgenteControladorLider l
        JOIN FETCH l.agente a
        JOIN FETCH l.controlador c
        JOIN FETCH l.plaza p
        WHERE p.id = :plazaId
          AND l.activo = true
        ORDER BY a.nombreCompleto ASC
        """)
    List<AgenteControladorLider> findLideresActivosPorPlaza(
            @Param("plazaId") Long plazaId
    );
}