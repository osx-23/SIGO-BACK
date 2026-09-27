package com.sigo.programacion.infrastructure.persistence.repository;

import com.sigo.programacion.infrastructure.persistence.entity.AgenteCasetaRestriccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgenteCasetaRestriccionRepository
        extends JpaRepository<AgenteCasetaRestriccion, Long> {

    List<AgenteCasetaRestriccion>
    findByTrabajadorPlazaIdAndActivoTrue(Long plazaId);

    Optional<AgenteCasetaRestriccion>
    findByTrabajadorIdAndUbicacionId(Long trabajadorId, Long ubicacionId);
}
