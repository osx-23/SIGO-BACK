-- Aplicar primero en la base de pruebas de SIGO. No se ejecuta automáticamente.
-- Una permuta contiene dos turnos/fechas/plazas: la obligación original
-- del solicitante y la obligación original del aceptante.
CREATE TABLE IF NOT EXISTS public.asistencia_permuta (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    asistencia_id BIGINT NOT NULL REFERENCES public.asistencia_registro(id) ON DELETE RESTRICT,
    solicitante_id BIGINT NOT NULL REFERENCES public.trabajadores(id),
    codigo_solicitante INTEGER NOT NULL,
    plaza_solicitante_id BIGINT NOT NULL REFERENCES public.plazas(id),
    turno_solicitante_id BIGINT NOT NULL REFERENCES public.turnos(id),
    fecha_solicitante DATE NOT NULL,
    aceptante_id BIGINT NOT NULL REFERENCES public.trabajadores(id),
    codigo_aceptante INTEGER NOT NULL,
    plaza_aceptante_id BIGINT NOT NULL REFERENCES public.plazas(id),
    turno_aceptante_id BIGINT NOT NULL REFERENCES public.turnos(id),
    fecha_aceptante DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_permuta_personas_distintas CHECK (solicitante_id <> aceptante_id)
);
CREATE INDEX IF NOT EXISTS idx_permuta_solicitante_fecha ON public.asistencia_permuta(solicitante_id, fecha_solicitante);
CREATE INDEX IF NOT EXISTS idx_permuta_aceptante_fecha ON public.asistencia_permuta(aceptante_id, fecha_aceptante);
CREATE INDEX IF NOT EXISTS idx_permuta_fecha_solicitante ON public.asistencia_permuta(fecha_solicitante);
CREATE INDEX IF NOT EXISTS idx_permuta_fecha_aceptante ON public.asistencia_permuta(fecha_aceptante);
-- El acceso se realiza exclusivamente por el backend Spring/JWT, nunca por Data API.
ALTER TABLE public.asistencia_permuta ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.asistencia_permuta FROM PUBLIC;
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
    REVOKE ALL ON public.asistencia_permuta FROM anon;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
    REVOKE ALL ON public.asistencia_permuta FROM authenticated;
  END IF;
END
$$;
