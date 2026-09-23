package com.sigo.asistencia.relevo.repository;
import com.sigo.asistencia.relevo.entity.ElementoRelevo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ElementoRelevoRepository extends JpaRepository<ElementoRelevo,Long> {
 List<ElementoRelevo> findByActivoTrueOrderByCategoriaAscOrdenAscNombreAsc();
}
