-- Endurecimiento para evitar que REST anon/authenticated consulte inventario.
-- SIGO obtiene estas vistas exclusivamente mediante la API Spring Boot.
ALTER VIEW public.v_inventario_stock_actual SET (security_invoker = true);
ALTER VIEW public.v_inventario_consumo_mensual SET (security_invoker = true);
REVOKE ALL ON public.v_inventario_stock_actual,
  public.v_inventario_consumo_mensual FROM anon, authenticated;
