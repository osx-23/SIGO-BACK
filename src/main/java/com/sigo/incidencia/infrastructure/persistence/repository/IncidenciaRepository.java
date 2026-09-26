package com.sigo.incidencia.infrastructure.persistence.repository;

import com.sigo.incidencia.domain.EstadoIncidencia;
import com.sigo.incidencia.infrastructure.persistence.entity.Incidencia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    @Override
    @EntityGraph(attributePaths = {"plaza", "turno", "registradoPor", "tipo", "via"})
    Optional<Incidencia> findById(Long id);

    @EntityGraph(attributePaths = {"plaza", "turno", "registradoPor", "tipo", "via"})
    List<Incidencia> findByFechaBetweenOrderByFechaDescHoraDesc(
            LocalDate inicio,
            LocalDate fin
    );

    @EntityGraph(attributePaths = {"plaza", "turno", "registradoPor", "tipo", "via"})
    List<Incidencia> findByFechaBetweenAndPlazaIdOrderByFechaDescHoraDesc(
            LocalDate inicio,
            LocalDate fin,
            Long plazaId
    );

    long countByEstado(EstadoIncidencia estado);
}
