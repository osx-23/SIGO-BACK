package com.sigo.relevo.infrastructure.persistence.repository;
import com.sigo.relevo.infrastructure.persistence.entity.RelevoChecklistEvidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RelevoChecklistEvidenciaRepository extends JpaRepository<RelevoChecklistEvidencia,Long> {
 List<RelevoChecklistEvidencia> findByChecklistIdOrderByIdAsc(Long checklistId);
 void deleteByChecklistId(Long checklistId);
}
