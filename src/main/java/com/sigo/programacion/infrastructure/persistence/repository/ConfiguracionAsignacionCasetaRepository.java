package com.sigo.programacion.infrastructure.persistence.repository;

import com.sigo.programacion.infrastructure.persistence.entity.ConfiguracionAsignacionCaseta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionAsignacionCasetaRepository
        extends JpaRepository<ConfiguracionAsignacionCaseta, Long> {

    Optional<ConfiguracionAsignacionCaseta> findByPlazaId(Long plazaId);
}
