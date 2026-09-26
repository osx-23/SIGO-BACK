package com.sigo.incidencia.infrastructure.persistence.repository;

import com.sigo.incidencia.infrastructure.persistence.entity.IncidenciaEvidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenciaEvidenciaRepository extends JpaRepository<IncidenciaEvidencia, Long> {
    List<IncidenciaEvidencia> findByIncidenciaIdOrderByIdAsc(Long incidenciaId);
}
