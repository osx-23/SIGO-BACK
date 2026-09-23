package com.sigo.asistencia.inventario.repository;
import com.sigo.asistencia.inventario.entity.InventarioRol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface InventarioRolRepository extends JpaRepository<InventarioRol,Long>{
  Optional<InventarioRol> findByCodigoAndActivoTrue(String codigo);
  List<InventarioRol> findByActivoTrueOrderByNombreAsc();
}
