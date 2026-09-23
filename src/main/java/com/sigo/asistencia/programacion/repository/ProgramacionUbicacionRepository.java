package com.sigo.asistencia.programacion.repository;

import com.sigo.asistencia.programacion.entity.ProgramacionUbicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgramacionUbicacionRepository
        extends JpaRepository<ProgramacionUbicacion, Long> {

    List<ProgramacionUbicacion> findByPlazaIdAndActivoTrueOrderByOrdenAscCodigoAsc(
            Long plazaId
    );

    List<ProgramacionUbicacion> findByPlazaIdOrderByOrdenAscCodigoAsc(
            Long plazaId
    );

    boolean existsByPlazaIdAndCodigoIgnoreCase(
            Long plazaId,
            String codigo
    );

    boolean existsByPlazaIdAndCodigoIgnoreCaseAndIdNot(
            Long plazaId,
            String codigo,
            Long id
    );
}
