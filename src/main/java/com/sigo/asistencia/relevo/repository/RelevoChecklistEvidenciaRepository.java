package com.sigo.asistencia.relevo.repository;
import com.sigo.asistencia.relevo.entity.RelevoChecklistEvidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RelevoChecklistEvidenciaRepository extends JpaRepository<RelevoChecklistEvidencia,Long> {
 List<RelevoChecklistEvidencia> findByChecklistIdOrderByIdAsc(Long checklistId);
}
