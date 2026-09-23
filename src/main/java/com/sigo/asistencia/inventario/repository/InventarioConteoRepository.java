package com.sigo.asistencia.inventario.repository;

import com.sigo.asistencia.inventario.entity.EstadoInventario;
import com.sigo.asistencia.inventario.entity.InventarioConteo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventarioConteoRepository
        extends JpaRepository<InventarioConteo, Long>,
        JpaSpecificationExecutor<InventarioConteo> {

  /*
   * Busca inventarios EN_PROCESO del trabajador.
   * Se usa al momento de iniciar un nuevo inventario
   * para evitar que tenga más de uno abierto.
   */
  @Query("""
        SELECT i
        FROM InventarioConteo i
        WHERE i.responsable.id = :responsableId
          AND i.estado = com.sigo.asistencia.inventario.entity.EstadoInventario.EN_PROCESO
        ORDER BY i.fechaInicio DESC
    """)
  Page<InventarioConteo> buscarEnProceso(
          @Param("responsableId") Long responsableId,
          Pageable pageable
  );


  /*
   * Obtiene un inventario específico.
   */
  @EntityGraph(attributePaths = {
          "plaza",
          "responsable",
          "responsable.puesto",
          "rol"
  })
  @Query("""
        SELECT i
        FROM InventarioConteo i
        WHERE i.id = :id
    """)
  Optional<InventarioConteo> findConDetalleById(
          @Param("id") Long id
  );
}