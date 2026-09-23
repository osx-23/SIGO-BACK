package com.sigo.asistencia.inventario.repository;

import com.sigo.asistencia.inventario.entity.InventarioProductoPlaza;
import com.sigo.asistencia.inventario.entity.InventarioProductoPlazaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventarioProductoPlazaRepository extends JpaRepository<InventarioProductoPlaza, InventarioProductoPlazaId> {

  void deleteByProductoId(Long productoId);

  List<InventarioProductoPlaza> findByProductoId(Long productoId);

  Optional<InventarioProductoPlaza> findByProductoIdAndPlazaId(Long productoId, Long plazaId);

  @Query("""
      select pp
      from InventarioProductoPlaza pp
      join fetch pp.plaza
      where pp.producto.id in :productoIds
      """)
  List<InventarioProductoPlaza> findAllByProductoIdsConPlaza(
      @Param("productoIds") Collection<Long> productoIds
  );
}
