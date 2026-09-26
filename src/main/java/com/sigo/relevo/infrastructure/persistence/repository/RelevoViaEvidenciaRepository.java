package com.sigo.relevo.infrastructure.persistence.repository;

import com.sigo.relevo.infrastructure.persistence.entity.RelevoViaEvidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelevoViaEvidenciaRepository
        extends JpaRepository<RelevoViaEvidencia, Long> {

    List<RelevoViaEvidencia> findByRelevoViaIdOrderByIdAsc(
            Long relevoViaId
    );

    List<RelevoViaEvidencia> findByRelevoViaIdInOrderByRelevoViaIdAscIdAsc(
            List<Long> relevoViaIds
    );

    void deleteByRelevoViaId(Long relevoViaId);
}
