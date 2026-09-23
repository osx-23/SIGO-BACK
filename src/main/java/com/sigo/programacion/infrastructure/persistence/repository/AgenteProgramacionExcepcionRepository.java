package com.sigo.programacion.infrastructure.persistence.repository;

import com.sigo.programacion.infrastructure.persistence.entity.AgenteProgramacionExcepcion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgenteProgramacionExcepcionRepository extends JpaRepository<AgenteProgramacionExcepcion, Long> {

    @EntityGraph(attributePaths = {"trabajador", "plaza"})
    List<AgenteProgramacionExcepcion> findByPlazaIdAndActivoTrueOrderByTrabajadorNombreCompletoAsc(Long plazaId);

    @EntityGraph(attributePaths = {"trabajador", "plaza"})
    Optional<AgenteProgramacionExcepcion> findByTrabajadorIdAndPlazaId(Long trabajadorId, Long plazaId);
}
