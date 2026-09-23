package com.sigo.asistencia.relevo.repository;
import com.sigo.asistencia.relevo.entity.RelevoVia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RelevoViaRepository extends JpaRepository<RelevoVia,Long> {
 List<RelevoVia> findByRelevoIdOrderByViaNumeroAsc(Long relevoId);
}
