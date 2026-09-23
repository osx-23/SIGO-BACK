package com.sigo.personal.infrastructure.persistence.repository;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlazaRepository extends JpaRepository<Plaza, Long> {
    List<Plaza> findByActivoTrueOrderByCodigoAsc();
}