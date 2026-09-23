package com.sigo.inventario.infrastructure.persistence.repository;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioPuestoRol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface InventarioPuestoRolRepository extends JpaRepository<InventarioPuestoRol,Long>{
  Optional<InventarioPuestoRol> findByPuestoId(Long puestoId);
}
