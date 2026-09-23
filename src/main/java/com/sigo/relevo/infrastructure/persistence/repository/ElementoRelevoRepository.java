package com.sigo.relevo.infrastructure.persistence.repository;
import com.sigo.relevo.infrastructure.persistence.entity.ElementoRelevo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ElementoRelevoRepository extends JpaRepository<ElementoRelevo,Long> {
 List<ElementoRelevo> findByActivoTrueOrderByCategoriaAscOrdenAscNombreAsc();
}
