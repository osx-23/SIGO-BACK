package com.sigo.inventario.infrastructure.persistence.repository;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioAmbito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InventarioAmbitoRepository extends JpaRepository<InventarioAmbito,Long>{
  List<InventarioAmbito> findByActivoTrueOrderByNombreAsc();
}
