package com.sigo.incidencia.infrastructure.persistence.repository;

import com.sigo.incidencia.infrastructure.persistence.entity.TipoIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoIncidenciaRepository extends JpaRepository<TipoIncidencia, Long> {
    List<TipoIncidencia> findByActivoTrueOrderByNombreAsc();
}
