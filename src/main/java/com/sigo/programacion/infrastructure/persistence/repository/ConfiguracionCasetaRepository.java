package com.sigo.programacion.infrastructure.persistence.repository;

import com.sigo.programacion.infrastructure.persistence.entity.ConfiguracionCaseta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionCasetaRepository
        extends JpaRepository<ConfiguracionCaseta, Long> {

    List<ConfiguracionCaseta> findByUbicacionPlazaId(Long plazaId);

    Optional<ConfiguracionCaseta> findByUbicacionId(Long ubicacionId);
}
