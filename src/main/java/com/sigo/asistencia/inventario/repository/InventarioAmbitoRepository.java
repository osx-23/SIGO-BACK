package com.sigo.asistencia.inventario.repository;
import com.sigo.asistencia.inventario.entity.InventarioAmbito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InventarioAmbitoRepository extends JpaRepository<InventarioAmbito,Long>{
  List<InventarioAmbito> findByActivoTrueOrderByNombreAsc();
}
