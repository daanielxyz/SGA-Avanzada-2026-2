-- Mínimo configurable de capacidades distintas entre los apartamentos activos (CAP-05 · DEC-51).
ALTER TABLE alojamiento ADD COLUMN minimo_capacidades_distintas INTEGER DEFAULT 2 NOT NULL;
ALTER TABLE alojamiento ADD CONSTRAINT ck_alojamiento_minimo_capacidades CHECK (minimo_capacidades_distintas >= 0);

-- Series de los códigos de negocio que genera el sistema (DEC-52). Una secuencia nunca repite un número aunque
-- dos transacciones la pidan a la vez, y no retrocede si una transacción se deshace.
CREATE SEQUENCE seq_servicio START WITH 1;
CREATE SEQUENCE seq_politica START WITH 1;
CREATE SEQUENCE seq_temporada START WITH 1;
CREATE SEQUENCE seq_tarifa START WITH 1;
CREATE SEQUENCE seq_bloqueo START WITH 1;

CREATE INDEX ix_apartamento_alojamiento ON apartamento (alojamiento_id, activo);
