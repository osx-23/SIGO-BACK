package com.sigo.inventario.infrastructure.persistence.repository;

import com.sigo.inventario.infrastructure.persistence.entity.EstadoInventario;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioConteo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventarioConteoRepository
        extends JpaRepository<InventarioConteo, Long>,
        JpaSpecificationExecutor<InventarioConteo> {


  /**
   * Caduca de manera atomica cualquier conteo que alcance 8 horas abierto.
   * No altera inventarios FINALIZADOS ni ANULADOS.
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(value = """
      UPDATE inventario_conteo
      SET estado = 'ANULADO',
          anulado_en = CURRENT_TIMESTAMP,
          motivo_anulacion = 'Anulado automaticamente: excedio el turno de 8 horas',
          actualizado_en = CURRENT_TIMESTAMP
      WHERE estado = 'EN_PROCESO'
        AND fecha_inicio <= CURRENT_TIMESTAMP - INTERVAL '8 hours'
      """, nativeQuery = true)
  int anularVencidos();

  @Query(value = """
      SELECT id FROM inventario_conteo
      WHERE id = :id
        AND estado = 'EN_PROCESO'
        AND fecha_inicio > CURRENT_TIMESTAMP - INTERVAL '8 hours'
      FOR UPDATE
      """, nativeQuery = true)
  java.util.List<Long> bloquearVigente(@Param("id") Long id);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(value = """
      UPDATE inventario_conteo
      SET estado = 'FINALIZADO',
          fecha_finalizacion = CURRENT_TIMESTAMP,
          actualizado_en = CURRENT_TIMESTAMP
      WHERE id = :id
        AND estado = 'EN_PROCESO'
        AND fecha_inicio > CURRENT_TIMESTAMP - INTERVAL '8 hours'
      """, nativeQuery = true)
  int finalizarSiVigente(@Param("id") Long id);

  /*
   * Busca inventarios EN_PROCESO del trabajador.
   * Se usa al momento de iniciar un nuevo inventario
   * para evitar que tenga más de uno abierto.
   */
  @Query("""
        SELECT i
        FROM InventarioConteo i
        WHERE i.responsable.id = :responsableId
          AND i.estado = com.sigo.inventario.infrastructure.persistence.entity.EstadoInventario.EN_PROCESO
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