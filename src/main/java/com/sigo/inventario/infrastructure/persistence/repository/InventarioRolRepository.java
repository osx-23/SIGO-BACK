package com.sigo.inventario.infrastructure.persistence.repository;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioRol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface InventarioRolRepository extends JpaRepository<InventarioRol,Long>{
  Optional<InventarioRol> findByCodigoAndActivoTrue(String codigo);
  List<InventarioRol> findByActivoTrueOrderByNombreAsc();
}
