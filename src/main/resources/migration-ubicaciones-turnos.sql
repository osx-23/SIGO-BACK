-- Disponibilidad de casetas/ubicaciones por turno.
-- Valores por defecto en true para mantener compatibilidad con la configuración existente.
ALTER TABLE programacion_ubicacion
    ADD COLUMN IF NOT EXISTS permite_turno_a BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS permite_turno_b BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS permite_turno_c BOOLEAN NOT NULL DEFAULT TRUE;
