package com.sigo.asistencia.infrastructure.persistence.repository;

import com.sigo.asistencia.infrastructure.persistence.entity.AsistenciaRegistro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AsistenciaRepository
        extends JpaRepository<AsistenciaRegistro, Long> {

 /*
  * ============================================================
  * VALIDACIONES DE REGISTRO
  * ============================================================
  */

 boolean existsByPlazaIdAndTurnoIdAndFecha(
         Long plazaId,
         Long turnoId,
         LocalDate fecha
 );

 boolean existsByPlazaIdAndTurnoIdAndFechaAndIdNot(
         Long plazaId,
         Long turnoId,
         LocalDate fecha,
         Long id
 );

 /*
  * ============================================================
  * HISTORIAL POR RANGO
  * ============================================================
  *
  * Carga en la misma consulta las relaciones que necesita
  * AsistenciaResponse para evitar consultas lazy adicionales.
  */

 @Query("""
            SELECT ar
            FROM AsistenciaRegistro ar
            JOIN FETCH ar.plaza p
            JOIN FETCH ar.turno t
            JOIN FETCH ar.controlador c
            WHERE ar.fecha BETWEEN :inicio AND :fin
            ORDER BY ar.fecha DESC, ar.id DESC
            """)
 List<AsistenciaRegistro> listarHistorial(
         @Param("inicio") LocalDate inicio,
         @Param("fin") LocalDate fin
 );

 /*
  * ============================================================
  * HISTORIAL POR RANGO + PLAZA
  * ============================================================
  */

 @Query("""
            SELECT ar
            FROM AsistenciaRegistro ar
            JOIN FETCH ar.plaza p
            JOIN FETCH ar.turno t
            JOIN FETCH ar.controlador c
            WHERE ar.fecha BETWEEN :inicio AND :fin
              AND ar.plaza.id = :plazaId
            ORDER BY ar.fecha DESC, ar.id DESC
            """)
 List<AsistenciaRegistro> listarHistorialPorPlaza(
         @Param("inicio") LocalDate inicio,
         @Param("fin") LocalDate fin,
         @Param("plazaId") Long plazaId
 );

 /*
  * ============================================================
  * DASHBOARD - ASISTENCIA DIARIA
  * ============================================================
  *
  * La consulta devuelve:
  *
  * [0] día
  * [1] presentes
  * [2] programados
  * [3] porcentaje
  *
  * plazaId:
  * null = todas las plazas
  *
  * turnoId:
  * null = todos los turnos
  */

 @Query(
         value = """
                       SELECT
                           EXTRACT(DAY FROM ar.fecha)::int,
                           COALESCE(SUM(ar.presentes), 0)::bigint,
                           COALESCE(SUM(ar.programados), 0)::bigint,
                           COALESCE(
                               ROUND(
                                   SUM(ar.presentes)::numeric
                                   / NULLIF(
                                       SUM(ar.programados),
                                       0
                                   )
                                   * 100,
                                   2
                               ),
                               0
                           )
                       FROM asistencia_registro ar
                       WHERE ar.fecha >= :inicio
                         AND ar.fecha <= :fin
                         AND (
                               :plazaId IS NULL
                               OR ar.plaza_id = :plazaId
                         )
                         AND (
                               :turnoId IS NULL
                               OR ar.turno_id = :turnoId
                         )
                       GROUP BY
                           EXTRACT(DAY FROM ar.fecha)
                       ORDER BY
                           EXTRACT(DAY FROM ar.fecha)
                       """,
         nativeQuery = true
 )
 List<Object[]> obtenerDiario(
         @Param("inicio") LocalDate inicio,
         @Param("fin") LocalDate fin,
         @Param("plazaId") Long plazaId,
         @Param("turnoId") Long turnoId
 );

 /*
  * ============================================================
  * DASHBOARD - ASISTENCIA ANUAL
  * ============================================================
  *
  * La consulta devuelve:
  *
  * [0] mes
  * [1] presentes
  * [2] programados
  * [3] porcentaje
  *
  * Como máximo devolverá 12 filas.
  */

 @Query(
         value = """
                       SELECT
                           EXTRACT(MONTH FROM ar.fecha)::int,
                           COALESCE(SUM(ar.presentes), 0)::bigint,
                           COALESCE(SUM(ar.programados), 0)::bigint,
                           COALESCE(
                               ROUND(
                                   SUM(ar.presentes)::numeric
                                   / NULLIF(
                                       SUM(ar.programados),
                                       0
                                   )
                                   * 100,
                                   2
                               ),
                               0
                           )
                       FROM asistencia_registro ar
                       WHERE ar.fecha >= :inicio
                         AND ar.fecha <= :fin
                         AND (
                               :plazaId IS NULL
                               OR ar.plaza_id = :plazaId
                         )
                         AND (
                               :turnoId IS NULL
                               OR ar.turno_id = :turnoId
                         )
                       GROUP BY
                           EXTRACT(MONTH FROM ar.fecha)
                       ORDER BY
                           EXTRACT(MONTH FROM ar.fecha)
                       """,
         nativeQuery = true
 )
 List<Object[]> obtenerAnual(
         @Param("inicio") LocalDate inicio,
         @Param("fin") LocalDate fin,
         @Param("plazaId") Long plazaId,
         @Param("turnoId") Long turnoId
 );

 /*
  * ============================================================
  * DASHBOARD - RESUMEN
  * ============================================================
  *
  * IMPORTANTE:
  *
  * Aunque SQL devuelve una única fila agregada,
  * mantenemos List<Object[]> para que Hibernate/Spring Data
  * maneje correctamente el resultado de la consulta nativa.
  *
  * La consulta devuelve:
  *
  * [0] cantidad de registros
  * [1] presentes
  * [2] programados
  * [3] ausentes
  */

 @Query(
         value = """
                       SELECT
                           COUNT(*)::bigint,
                           COALESCE(
                               SUM(ar.presentes),
                               0
                           )::bigint,
                           COALESCE(
                               SUM(ar.programados),
                               0
                           )::bigint,
                           COALESCE(
                               SUM(
                                   ar.programados - ar.presentes
                               ),
                               0
                           )::bigint
                       FROM asistencia_registro ar
                       WHERE ar.fecha >= :inicio
                         AND ar.fecha <= :fin
                         AND (
                               :plazaId IS NULL
                               OR ar.plaza_id = :plazaId
                         )
                         AND (
                               :turnoId IS NULL
                               OR ar.turno_id = :turnoId
                         )
                       """,
         nativeQuery = true
 )
 List<Object[]> obtenerResumen(
         @Param("inicio") LocalDate inicio,
         @Param("fin") LocalDate fin,
         @Param("plazaId") Long plazaId,
         @Param("turnoId") Long turnoId
 );
}
