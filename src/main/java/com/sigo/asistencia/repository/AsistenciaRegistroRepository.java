package com.sigo.asistencia.repository;

import com.sigo.asistencia.entity.AsistenciaRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AsistenciaRegistroRepository
        extends JpaRepository<AsistenciaRegistro, Long> {

    /*
     * Verifica si ya existe un registro
     * para la misma plaza, turno y fecha.
     */
    boolean existsByPlazaIdAndTurnoIdAndFecha(
            Long plazaId,
            Long turnoId,
            LocalDate fecha
    );

    /*
     * Historial:
     * buscar todas las asistencias dentro
     * de un rango de fechas.
     *
     * Se usa cuando:
     * Plaza = Todas las plazas
     */
    List<AsistenciaRegistro>
    findByFechaBetweenOrderByFechaDescIdDesc(
            LocalDate inicio,
            LocalDate fin
    );

    /*
     * Historial:
     * buscar asistencias dentro de un rango
     * de fechas y de una plaza específica.
     *
     * Se usa cuando el usuario selecciona
     * una plaza.
     */
    List<AsistenciaRegistro>
    findByFechaBetweenAndPlazaIdOrderByFechaDescIdDesc(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId
    );
}