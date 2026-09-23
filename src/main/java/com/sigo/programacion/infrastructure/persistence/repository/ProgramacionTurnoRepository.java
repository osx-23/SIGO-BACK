package com.sigo.programacion.infrastructure.persistence.repository;

import com.sigo.programacion.infrastructure.persistence.entity.EstadoProgramacion;
import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionTurno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProgramacionTurnoRepository
        extends JpaRepository<ProgramacionTurno, Long> {

    interface TurnoResumen {
        Long getProgramacionId();
        Long getTrabajadorId();
        Integer getCodigoTrabajador();
        String getNombreTrabajador();
        Long getPlazaId();
        String getPlazaCodigo();
        LocalDate getFecha();
        EstadoProgramacion getEstado();
    }

    interface HorarioCompletoResumen {
        Long getTrabajadorId();
        Integer getCodigoTrabajador();
        String getNombreTrabajador();
        Long getPlazaId();
        String getPlazaCodigo();
        Long getProgramacionId();
        LocalDate getFecha();
        EstadoProgramacion getEstado();
        String getUbicacionCodigo();
        String getUbicacionNombre();
        String getLiderNombre();
    }

    Optional<ProgramacionTurno>
    findByTrabajadorIdAndFecha(
            Long trabajadorId,
            LocalDate fecha
    );

    /*
     * Proyección de solo lectura para la matriz mensual.
     * Evita hidratar entidades completas de trabajador/plaza
     * cuando la API solo necesita estos ocho campos.
     */
    @Query("""
        select
            p.id as programacionId,
            t.id as trabajadorId,
            t.codigo as codigoTrabajador,
            t.nombreCompleto as nombreTrabajador,
            pl.id as plazaId,
            pl.codigo as plazaCodigo,
            p.fecha as fecha,
            p.estado as estado
        from ProgramacionTurno p
        join p.trabajador t
        join p.plaza pl
        where pl.id = :plazaId
          and p.fecha between :desde and :hasta
        order by t.nombreCompleto asc, p.fecha asc
    """)
    List<TurnoResumen> findMesResumen(
            @Param("plazaId") Long plazaId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    /*
     * Lectura completa de "Mi horario" en una sola consulta.
     *
     * La consulta parte del trabajador para conservar sus datos incluso
     * cuando no tenga programación dentro del rango solicitado.
     */
    @Query("""
        select
            t.id as trabajadorId,
            t.codigo as codigoTrabajador,
            t.nombreCompleto as nombreTrabajador,
            pl.id as plazaId,
            pl.codigo as plazaCodigo,
            p.id as programacionId,
            p.fecha as fecha,
            p.estado as estado,
            u.codigo as ubicacionCodigo,
            u.nombre as ubicacionNombre,
            c.nombreCompleto as liderNombre
        from Trabajador t
        left join t.plaza pl
        left join ProgramacionTurno p
            on p.trabajador = t
           and p.fecha between :desde and :hasta
        left join DistribucionPersonal d
            on d.programacionTurno = p
        left join d.ubicacion u
        left join AgenteControladorLider l
            on l.agente = t
           and l.activo = true
        left join l.controlador c
        where t.id = :trabajadorId
        order by p.fecha asc
    """)
    List<HorarioCompletoResumen> findHorarioCompleto(
            @Param("trabajadorId") Long trabajadorId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    /*
     * Proyección reducida para "Mi horario".
     */
    @Query("""
        select
            p.id as programacionId,
            t.id as trabajadorId,
            t.codigo as codigoTrabajador,
            t.nombreCompleto as nombreTrabajador,
            pl.id as plazaId,
            pl.codigo as plazaCodigo,
            p.fecha as fecha,
            p.estado as estado
        from ProgramacionTurno p
        join p.trabajador t
        join p.plaza pl
        where t.id = :trabajadorId
          and p.fecha between :desde and :hasta
        order by p.fecha asc
    """)
    List<TurnoResumen> findHorarioResumen(
            @Param("trabajadorId") Long trabajadorId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    /*
     * Se conservan las consultas por entidad porque siguen siendo
     * útiles en flujos de escritura y compatibilidad interna.
     */
    @Query("""
        select p from ProgramacionTurno p
        join fetch p.trabajador t
        join fetch p.plaza pl
        where pl.id=:plazaId
          and p.fecha between :desde and :hasta
        order by t.nombreCompleto asc, p.fecha asc
    """)
    List<ProgramacionTurno> findMes(
            @Param("plazaId") Long plazaId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    @Query("""
        select p from ProgramacionTurno p
        join fetch p.trabajador t
        join fetch p.plaza pl
        where t.id=:trabajadorId
          and p.fecha between :desde and :hasta
        order by p.fecha asc
    """)
    List<ProgramacionTurno> findHorario(
            @Param("trabajadorId") Long trabajadorId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );

    @Query("""
        select p from ProgramacionTurno p
        join fetch p.trabajador t
        join fetch p.plaza pl
        where pl.id = :plazaId
          and t.id in :trabajadorIds
          and p.fecha between :desde and :hasta
    """)
    List<ProgramacionTurno> findParaGuardadoMasivo(
            @Param("plazaId") Long plazaId,
            @Param("trabajadorIds") Set<Long> trabajadorIds,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );
}
