package com.sigo.asistencia.personal.repository;

import com.sigo.asistencia.personal.entity.Plaza;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlazaRepository extends JpaRepository<Plaza, Long> {
    List<Plaza> findByActivoTrueOrderByCodigoAsc();
}