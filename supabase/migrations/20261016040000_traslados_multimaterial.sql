-- Agrupa varios materiales en una misma transferencia, sin alterar los movimientos existentes.
ALTER TABLE public.inventario_traslado
  ADD COLUMN IF NOT EXISTS grupo_id UUID;

-- Cada traslado anterior queda como un envío independiente.
UPDATE public.inventario_traslado
  SET grupo_id = gen_random_uuid()
  WHERE grupo_id IS NULL;

ALTER TABLE public.inventario_traslado
  ALTER COLUMN grupo_id SET DEFAULT gen_random_uuid(),
  ALTER COLUMN grupo_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_inventario_traslado_grupo_producto
  ON public.inventario_traslado(grupo_id,producto_id);
CREATE INDEX IF NOT EXISTS idx_inventario_traslado_grupo
  ON public.inventario_traslado(grupo_id);
