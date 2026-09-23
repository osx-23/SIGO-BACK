package com.sigo.relevo.infrastructure.persistence.repository;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RelevoChecklistRepository extends JpaRepository<RelevoChecklist,Long> {
 List<RelevoChecklist> findByRelevoIdOrderByElementoCategoriaAscElementoOrdenAsc(Long relevoId);
}
