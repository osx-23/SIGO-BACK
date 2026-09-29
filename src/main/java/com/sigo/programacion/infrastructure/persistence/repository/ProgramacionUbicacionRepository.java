package com.sigo.programacion.infrastructure.persistence.repository;

import com.sigo.programacion.infrastructure.persistence.entity.ProgramacionUbicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ProgramacionUbicacionRepository
        extends JpaRepository<ProgramacionUbicacion, Long> {

    @Query("""
        select u from ProgramacionUbicacion u
        join fetch u.plaza pl
        where u.id in :ids
    """)
    List<ProgramacionUbicacion> findAllParaDistribucion(
            @Param("ids") Set<Long> ids
    );


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
