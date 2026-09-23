package com.sigo.inventario.infrastructure.persistence.repository;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioProducto;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface InventarioProductoRepository extends JpaRepository<InventarioProducto,Long>{
  Optional<InventarioProducto> findByCodigoIgnoreCase(String codigo);

  @Query("""
    select distinct p from InventarioProducto p
    join InventarioProductoRol pr on pr.producto.id=p.id
    join InventarioProductoPlaza pp on pp.producto.id=p.id
    where p.activo=true and pr.rol.id=:rolId and pp.plaza.id=:plazaId
    order by p.nombre
  """)
  List<InventarioProducto> buscarPermitidos(@Param("rolId") Long rolId,@Param("plazaId") Long plazaId);

  @Query("""
    select case when count(p)>0 then true else false end
    from InventarioProducto p
    join InventarioProductoRol pr on pr.producto.id=p.id
    join InventarioProductoPlaza pp on pp.producto.id=p.id
    where p.id=:productoId and p.activo=true and pr.rol.id=:rolId and pp.plaza.id=:plazaId
  """)
  boolean esVisiblePara(@Param("productoId") Long productoId,@Param("rolId") Long rolId,@Param("plazaId") Long plazaId);

  @Query("select distinct p from InventarioProducto p left join fetch p.categoria left join fetch p.ambito order by p.nombre")
  List<InventarioProducto> listarAdministracion();

  @Query("""
    select distinct p from InventarioProducto p
    left join fetch p.categoria
    left join fetch p.ambito
    join InventarioProductoPlaza pp on pp.producto.id = p.id
    where pp.plaza.id = :plazaId
    order by p.nombre
  """)
  List<InventarioProducto> listarAdministracionPorPlaza(@Param("plazaId") Long plazaId);
}
