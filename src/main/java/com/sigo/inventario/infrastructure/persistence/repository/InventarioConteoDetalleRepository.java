package com.sigo.inventario.infrastructure.persistence.repository;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioConteoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface InventarioConteoDetalleRepository extends JpaRepository<InventarioConteoDetalle,Long>{
  List<InventarioConteoDetalle> findByInventarioIdOrderByNombreProductoSnapshotAsc(Long inventarioId);
  Optional<InventarioConteoDetalle> findByInventarioIdAndProductoId(Long inventarioId,Long productoId);
  long countByInventarioId(Long inventarioId);
}
