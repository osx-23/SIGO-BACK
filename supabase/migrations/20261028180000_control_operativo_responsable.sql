-- Mantener trazabilidad: registrado_por_id es el actor de la sesión; responsable_id es editable.
-- Nullable durante el despliegue para no romper versiones anteriores del backend.
ALTER TABLE public.control_operativo_reportes
  ADD COLUMN IF NOT EXISTS responsable_id bigint;

UPDATE public.control_operativo_reportes
SET responsable_id = registrado_por_id
WHERE responsable_id IS NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint
    WHERE conrelid = 'public.control_operativo_reportes'::regclass
      AND conname = 'control_operativo_reportes_responsable_id_fkey'
  ) THEN
    ALTER TABLE public.control_operativo_reportes
      ADD CONSTRAINT control_operativo_reportes_responsable_id_fkey
      FOREIGN KEY (responsable_id) REFERENCES public.trabajadores(id);
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_control_operativo_reportes_responsable
  ON public.control_operativo_reportes (responsable_id);

COMMENT ON COLUMN public.control_operativo_reportes.responsable_id IS
  'Responsable supervisor o controlador del control operativo, editable; distinto de registrado_por_id (auditoría).';
