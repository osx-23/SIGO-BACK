package com.sigo.asistencia.inventario.repository;

import com.sigo.asistencia.inventario.entity.InventarioProductoRol;
import com.sigo.asistencia.inventario.entity.InventarioProductoRolId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface InventarioProductoRolRepository extends JpaRepository<InventarioProductoRol, InventarioProductoRolId> {

  void deleteByProductoId(Long productoId);

  List<InventarioProductoRol> findByProductoId(Long productoId);

  @Query("""
      select pr
      from InventarioProductoRol pr
      join fetch pr.rol
      where pr.producto.id in :productoIds
      """)
  List<InventarioProductoRol> findAllByProductoIdsConRol(
      @Param("productoIds") Collection<Long> productoIds
  );
}
