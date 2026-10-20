-- Ejecutar UNA VEZ en la base de datos de pruebas antes de desplegar el flujo de sesiones.
-- No modifica ni elimina relevos existentes.
CREATE TABLE IF NOT EXISTS relevo_sesiones (
  id BIGSERIAL PRIMARY KEY,
  operador_id BIGINT NOT NULL,
  plaza_id BIGINT NOT NULL,
  turno_id BIGINT NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'EN_PROCESO',
  iniciado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finalizado_en TIMESTAMPTZ,
  relevo_id BIGINT UNIQUE REFERENCES relevos(id),
  borrador_json TEXT,
  CONSTRAINT ck_relevo_sesion_estado CHECK (estado IN ('EN_PROCESO', 'FINALIZADO', 'ANULADO'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_relevo_sesion_activa_usuario
  ON relevo_sesiones (operador_id) WHERE estado = 'EN_PROCESO';

CREATE INDEX IF NOT EXISTS ix_relevo_sesiones_relevo
  ON relevo_sesiones (relevo_id) WHERE relevo_id IS NOT NULL;
