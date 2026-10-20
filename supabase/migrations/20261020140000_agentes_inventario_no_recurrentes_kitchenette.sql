-- Habilitar materiales NO RECURRENTES y KITCHENETTE para el rol AGENTE.
-- Se conservan el estado activo, la pertenencia a plaza y las asignaciones existentes.
INSERT INTO inventario_producto_rol (producto_id, rol_id)
SELECT p.id, r.id
FROM inventario_producto p
JOIN inventario_rol r ON UPPER(r.codigo) = 'AGENTE' AND r.activo = TRUE
LEFT JOIN inventario_ambito a ON a.id = p.ambito_id
LEFT JOIN inventario_categoria c ON c.id = p.categoria_id
WHERE p.activo = TRUE
  AND (
    UPPER(COALESCE(a.codigo, '')) = 'NO_RECURRENTES'
    OR UPPER(COALESCE(c.nombre, '')) IN ('KITCHENETTE', 'KITCHENET')
  )
  AND EXISTS (
    SELECT 1 FROM inventario_producto_plaza pp
    WHERE pp.producto_id = p.id
  )
ON CONFLICT (producto_id, rol_id) DO NOTHING;
