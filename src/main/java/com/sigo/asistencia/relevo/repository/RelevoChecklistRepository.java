package com.sigo.asistencia.relevo.repository;
import com.sigo.asistencia.relevo.entity.RelevoChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RelevoChecklistRepository extends JpaRepository<RelevoChecklist,Long> {
 List<RelevoChecklist> findByRelevoIdOrderByElementoCategoriaAscElementoOrdenAsc(Long relevoId);
}
