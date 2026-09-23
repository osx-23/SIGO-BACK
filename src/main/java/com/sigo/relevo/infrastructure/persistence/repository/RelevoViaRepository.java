package com.sigo.relevo.infrastructure.persistence.repository;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoVia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RelevoViaRepository extends JpaRepository<RelevoVia,Long> {
 List<RelevoVia> findByRelevoIdOrderByViaNumeroAsc(Long relevoId);
}
