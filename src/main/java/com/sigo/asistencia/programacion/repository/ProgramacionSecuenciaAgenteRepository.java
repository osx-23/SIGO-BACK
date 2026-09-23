package com.sigo.asistencia.programacion.repository;

import com.sigo.asistencia.programacion.entity.GrupoProgramacion;
import com.sigo.asistencia.programacion.entity.ProgramacionSecuenciaAgente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProgramacionSecuenciaAgenteRepository
        extends JpaRepository<ProgramacionSecuenciaAgente, Long> {

    Optional<ProgramacionSecuenciaAgente> findByAgenteId(Long agenteId);

    /*
     * Se mantiene por compatibilidad con cualquier otro punto del proyecto
     * que todavía utilice el método derivado original.
     */
    List<ProgramacionSecuenciaAgente>
    findByPlazaIdOrderByGrupoAscOrdenAsc(Long plazaId);

    List<ProgramacionSecuenciaAgente>
    findByPlazaIdAndGrupoOrderByOrdenAsc(
            Long plazaId,
            GrupoProgramacion grupo
    );

    long countByPlazaIdAndGrupo(
            Long plazaId,
            GrupoProgramacion grupo
    );

    /*
     * Consulta optimizada para la pantalla de programación.
     * JOIN FETCH evita consultas adicionales al acceder a agente y plaza.
     * Además, una secuencia vigente nunca debe mostrar agentes inactivos.
     */
    @Query("""
            SELECT s
            FROM ProgramacionSecuenciaAgente s
            JOIN FETCH s.agente a
            JOIN FETCH s.plaza p
            WHERE p.id = :plazaId
              AND a.activo = true
            ORDER BY s.grupo ASC NULLS LAST,
                     s.orden ASC NULLS LAST,
                     a.nombreCompleto ASC
            """)
    List<ProgramacionSecuenciaAgente> findActivasByPlazaId(
            @Param("plazaId") Long plazaId
    );

    /*
     * Variante para trabajar con un grupo concreto durante asignación,
     * normalización y reordenamiento.
     */
    @Query("""
            SELECT s
            FROM ProgramacionSecuenciaAgente s
            JOIN FETCH s.agente a
            JOIN FETCH s.plaza p
            WHERE p.id = :plazaId
              AND s.grupo = :grupo
              AND a.activo = true
            ORDER BY s.orden ASC NULLS LAST,
                     a.nombreCompleto ASC
            """)
    List<ProgramacionSecuenciaAgente> findActivasByPlazaIdAndGrupo(
            @Param("plazaId") Long plazaId,
            @Param("grupo") GrupoProgramacion grupo
    );

    /*
     * programacion_secuencia_agente es configuración vigente, no histórico.
     * Si un trabajador fue desactivado, su fila antigua no debe seguir
     * ocupando una posición de secuencia. El DELETE evita ejecutar el trigger
     * de validación que está rechazando UPDATEs del agente inactivo.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM ProgramacionSecuenciaAgente s
            WHERE s.plaza.id = :plazaId
              AND s.agente.activo = false
            """)
    int deleteInactivasByPlazaId(
            @Param("plazaId") Long plazaId
    );
}
