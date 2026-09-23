package com.sigo.inventario.infrastructure.persistence.repository;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InventarioCategoriaRepository extends JpaRepository<InventarioCategoria,Long>{
  List<InventarioCategoria> findByActivoTrueOrderByNombreAsc();
}
