-- Migración complementaria para importar el histórico 2026 sin generar
-- falsos registros de asistencia ni usuarios que no existan en SIGO.
-- Aplicada en SIGO-TEST; mantener para recreación de entornos.
ALTER TABLE public.asistencia_permuta
  ALTER COLUMN asistencia_id DROP NOT NULL,
  ALTER COLUMN solicitante_id DROP NOT NULL,
  ALTER COLUMN aceptante_id DROP NOT NULL;
ALTER TABLE public.asistencia_permuta
  ADD COLUMN IF NOT EXISTS origen varchar(32) NOT NULL DEFAULT 'REGISTRO_ASISTENCIA',
  ADD COLUMN IF NOT EXISTS archivo_origen varchar(160),
  ADD COLUMN IF NOT EXISTS fila_origen integer,
  ADD COLUMN IF NOT EXISTS nombre_solicitante_historico varchar(250),
  ADD COLUMN IF NOT EXISTS nombre_aceptante_historico varchar(250),
  ADD COLUMN IF NOT EXISTS observacion_importacion varchar(300);
CREATE UNIQUE INDEX IF NOT EXISTS asistencia_permuta_archivo_fila_unique
  ON public.asistencia_permuta (archivo_origen, fila_origen)
  WHERE archivo_origen IS NOT NULL;
-- No reemplazar la restricción original de personas distintas:
-- los registros incoherentes del archivo se preservan solo como históricos.
