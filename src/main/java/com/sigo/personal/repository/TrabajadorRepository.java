package com.sigo.personal.repository;

import com.sigo.personal.entity.Trabajador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrabajadorRepository
        extends JpaRepository<Trabajador, Long> {

    /*
     * ============================================================
     * CONSULTAS GENERALES
     * ============================================================
     */

    Optional<Trabajador> findByCodigo(Integer codigo);

    List<Trabajador> findByActivoTrueOrderByNombreCompletoAsc();

    List<Trabajador> findAllByOrderByNombreCompletoAsc();

    List<Trabajador>
    findByPuestoNombreIgnoreCaseAndActivoTrueOrderByNombreCompletoAsc(
            String puesto
    );

    /*
     * ============================================================
     * ADMINISTRACIÓN DE USUARIOS - OPTIMIZADO
     * ============================================================
     *
     * Se cargan puesto y plaza mediante JOIN FETCH.
     *
     * Esto evita el problema N+1:
     *
     * 1 consulta trabajadores
     * + N consultas puestos
     * + N consultas plazas
     *
     * Ahora se obtiene todo en una sola consulta.
     * ============================================================
     */

    @Query("""
        SELECT DISTINCT t
        FROM Trabajador t
        LEFT JOIN FETCH t.puesto
        LEFT JOIN FETCH t.plaza
        ORDER BY t.nombreCompleto ASC
    """)
    List<Trabajador> findAllAdminOptimizado();

    /*
     * Misma consulta administrativa pero filtrando directamente
     * en PostgreSQL por plaza.
     *
     * Ejemplo:
     *
     * GET /api/trabajadores/admin?plazaId=4
     */

    @Query("""
        SELECT DISTINCT t
        FROM Trabajador t
        LEFT JOIN FETCH t.puesto
        LEFT JOIN FETCH t.plaza pl
        WHERE pl.id = :plazaId
        ORDER BY t.nombreCompleto ASC
    """)
    List<Trabajador> findAllAdminByPlazaOptimizado(
            @Param("plazaId") Long plazaId
    );

    /*
     * ============================================================
     * AGENTES POR PLAZA
     * ============================================================
     */

    @Query("""
        SELECT t
        FROM Trabajador t
        JOIN FETCH t.puesto p
        LEFT JOIN FETCH t.plaza
        WHERE t.plaza.id = :plazaId
          AND t.activo = true
          AND p.nombre IN (
              'Agente de Recaudación',
              'Agente de Recaudacion - Part Time',
              'Agente de Recaudación Suplencia'
          )
        ORDER BY t.nombreCompleto ASC
    """)
    List<Trabajador> findAgentesByPlaza(
            @Param("plazaId") Long plazaId
    );

    /*
     * ============================================================
     * CONTROLADORES POR PLAZA
     * ============================================================
     */

    @Query("""
        SELECT t
        FROM Trabajador t
        JOIN FETCH t.puesto p
        LEFT JOIN FETCH t.plaza
        WHERE t.plaza.id = :plazaId
          AND t.activo = true
          AND p.nombre IN (
              'Controlador',
              'Controlador ATF'
          )
        ORDER BY t.nombreCompleto ASC
    """)
    List<Trabajador> findControladoresByPlaza(
            @Param("plazaId") Long plazaId
    );

    /*
     * ============================================================
     * TODOS LOS CONTROLADORES ACTIVOS
     * ============================================================
     */

    @Query("""
        SELECT t
        FROM Trabajador t
        JOIN FETCH t.puesto p
        LEFT JOIN FETCH t.plaza
        WHERE t.activo = true
          AND p.nombre IN (
              'Controlador',
              'Controlador ATF'
          )
        ORDER BY t.nombreCompleto ASC
    """)
    List<Trabajador> findControladoresActivos();
}