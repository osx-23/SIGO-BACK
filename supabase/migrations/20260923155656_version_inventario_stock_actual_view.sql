-- Versiona en el repositorio la vista de stock actual usada por SIGO-TEST.
-- Mantiene un único stock vigente por producto y plaza, tomando
-- el inventario FINALIZADO más reciente.

CREATE OR REPLACE VIEW public.v_inventario_stock_actual AS
SELECT
    i.plaza_id,
    p.codigo AS plaza,
    d.producto_id,
    d.nombre_producto_snapshot AS producto,
    d.unidad_snapshot AS unidad_medida,
    d.cantidad_encontrada AS cantidad_actual,
    i.id AS inventario_id,
    i.fecha_finalizacion AS actualizado_en
FROM public.inventario_conteo i
JOIN public.inventario_conteo_detalle d
    ON d.inventario_id = i.id
JOIN public.plazas p
    ON p.id = i.plaza_id
WHERE i.estado::text = 'FINALIZADO'
  AND i.id = (
      SELECT i2.id
      FROM public.inventario_conteo i2
      JOIN public.inventario_conteo_detalle d2
          ON d2.inventario_id = i2.id
      WHERE i2.plaza_id = i.plaza_id
        AND d2.producto_id = d.producto_id
        AND i2.estado::text = 'FINALIZADO'
      ORDER BY i2.fecha_finalizacion DESC, i2.id DESC
      LIMIT 1
  );
