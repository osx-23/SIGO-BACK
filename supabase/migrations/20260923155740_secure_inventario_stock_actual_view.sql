-- Hace que la vista ejecute las consultas con los permisos
-- del usuario que la invoca, evitando SECURITY DEFINER implícito.

ALTER VIEW public.v_inventario_stock_actual
SET (security_invoker = true);
