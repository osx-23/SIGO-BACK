package com.sigo.asistencia.inventario.repository;
import com.sigo.asistencia.inventario.entity.InventarioPuestoRol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface InventarioPuestoRolRepository extends JpaRepository<InventarioPuestoRol,Long>{
  Optional<InventarioPuestoRol> findByPuestoId(Long puestoId);
}
