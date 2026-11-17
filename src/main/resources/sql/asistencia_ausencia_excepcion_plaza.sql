-- Permite identificar ausencias excepcionales de agentes asignados a otra plaza.
-- No altera registros anteriores ni la plaza del trabajador.
ALTER TABLE public.asistencia_ausencia
    ADD COLUMN IF NOT EXISTS excepcion_plaza boolean NOT NULL DEFAULT false;
COMMENT ON COLUMN public.asistencia_ausencia.excepcion_plaza IS
    'true: ausencia de agente de recaudación de otra plaza, autorizada por el registrador';
