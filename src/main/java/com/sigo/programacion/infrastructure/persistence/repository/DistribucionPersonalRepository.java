package com.sigo.programacion.infrastructure.persistence.repository;

import com.sigo.programacion.infrastructure.persistence.entity.DistribucionPersonal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DistribucionPersonalRepository extends JpaRepository<DistribucionPersonal,Long> {
    Optional<DistribucionPersonal> findByProgramacionTurnoId(Long programacionTurnoId);

    @Query("""
        select d from DistribucionPersonal d
        join fetch d.programacionTurno p
        join fetch p.trabajador t
        join fetch p.plaza pl
        join fetch d.ubicacion u
        where pl.id=:plazaId and p.fecha between :desde and :hasta
        order by t.nombreCompleto asc, p.fecha asc
    """)
    List<DistribucionPersonal> findMes(@Param("plazaId") Long plazaId,@Param("desde") LocalDate desde,@Param("hasta") LocalDate hasta);

    @Query("""
        select d from DistribucionPersonal d
        join fetch d.programacionTurno p
        join fetch p.trabajador t
        join fetch d.ubicacion u
        where t.id=:trabajadorId and p.fecha between :desde and :hasta
        order by p.fecha asc
    """)
    List<DistribucionPersonal> findByTrabajadorMes(@Param("trabajadorId") Long trabajadorId,@Param("desde") LocalDate desde,@Param("hasta") LocalDate hasta);
}
