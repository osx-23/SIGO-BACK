package com.sigo.asistencia.relevo.repository;
import com.sigo.asistencia.relevo.entity.RelevoViaEvidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RelevoViaEvidenciaRepository extends JpaRepository<RelevoViaEvidencia,Long> {
 List<RelevoViaEvidencia> findByRelevoViaIdOrderByIdAsc(Long relevoViaId);
}
