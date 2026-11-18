-- SIGO | Edición y anulación auditada de permutas.
-- Ejecutar UNA sola vez en la base de pruebas antes de desplegar el backend.
-- Requiere permutas.sql y permutas_historico_2026.sql.
-- Cambios reversibles sin eliminación de los registros anteriores.

ALTER TABLE public.asistencia_permuta
  ADD COLUMN IF NOT EXISTS modificado_en TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS modificado_por VARCHAR(150),
  ADD COLUMN IF NOT EXISTS anulado_en TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS anulado_por VARCHAR(150);

CREATE TABLE IF NOT EXISTS public.asistencia_permuta_cambio (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  permuta_id BIGINT NOT NULL REFERENCES public.asistencia_permuta(id) ON DELETE RESTRICT,
  accion VARCHAR(20) NOT NULL CHECK (accion IN ('CORRECCION', 'ANULACION')),
  usuario VARCHAR(150) NOT NULL,
  registro_anterior JSONB NOT NULL,
  registro_nuevo JSONB NOT NULL,
  registrado_en TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_asistencia_permuta_activa
  ON public.asistencia_permuta (fecha_solicitante, fecha_aceptante)
  WHERE anulado_en IS NULL;

CREATE INDEX IF NOT EXISTS idx_asistencia_permuta_cambio_permuta
  ON public.asistencia_permuta_cambio (permuta_id, registrado_en DESC);

ALTER TABLE public.asistencia_permuta_cambio ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.asistencia_permuta_cambio FROM PUBLIC;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
    REVOKE ALL ON public.asistencia_permuta_cambio FROM anon;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
    REVOKE ALL ON public.asistencia_permuta_cambio FROM authenticated;
  END IF;
END
$$;
