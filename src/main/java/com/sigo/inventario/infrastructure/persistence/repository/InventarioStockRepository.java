package com.sigo.inventario.infrastructure.persistence.repository;

import com.sigo.inventario.application.port.in.InventarioStockUseCase;
import com.sigo.inventario.application.port.out.InventarioStockQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class InventarioStockRepository implements InventarioStockQueryPort {
  private final NamedParameterJdbcTemplate jdbc;

  @Override
  public List<InventarioStockUseCase.Stock> buscar(Long plazaId, String texto) {
    String sql = """
      SELECT s.plaza_id,s.plaza,s.producto_id,s.producto,s.unidad_medida,s.cantidad_actual,
             COALESCE(pp.stock_minimo,0) stock_minimo,
             (s.cantidad_actual < COALESCE(pp.stock_minimo,0)) bajo_minimo,
             s.inventario_id,s.actualizado_en
      FROM v_inventario_stock_actual s
      LEFT JOIN inventario_producto_plaza pp ON pp.producto_id=s.producto_id AND pp.plaza_id=s.plaza_id
      WHERE (:plazaId IS NULL OR s.plaza_id=:plazaId)
        AND (:texto IS NULL OR LOWER(s.producto) LIKE LOWER(CONCAT('%',CAST(:texto AS TEXT),'%')))
      ORDER BY s.plaza,s.producto
      """;

    String textoNormalizado = texto == null || texto.isBlank() ? null : texto.trim();
    var params = new MapSqlParameterSource()
      .addValue("plazaId", plazaId, Types.BIGINT)
      .addValue("texto", textoNormalizado, Types.VARCHAR);

    return jdbc.query(sql, params, (rs, n) -> new InventarioStockUseCase.Stock(
      rs.getLong("plaza_id"), rs.getString("plaza"), rs.getLong("producto_id"), rs.getString("producto"),
      rs.getString("unidad_medida"), rs.getBigDecimal("cantidad_actual"), rs.getInt("stock_minimo"),
      rs.getBoolean("bajo_minimo"), rs.getLong("inventario_id"),
      rs.getObject("actualizado_en", java.time.OffsetDateTime.class)));
  }
}
