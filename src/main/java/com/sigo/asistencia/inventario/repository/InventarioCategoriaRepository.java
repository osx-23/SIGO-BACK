package com.sigo.asistencia.inventario.repository;
import com.sigo.asistencia.inventario.entity.InventarioCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InventarioCategoriaRepository extends JpaRepository<InventarioCategoria,Long>{
  List<InventarioCategoria> findByActivoTrueOrderByNombreAsc();
}
