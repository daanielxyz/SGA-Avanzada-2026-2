-- Solo PostgreSQL (DEC-12 · DEC-17): última defensa del riesgo central en la base de datos.
-- Dos reservas activas del mismo apartamento no pueden compartir una noche (RN-01 · EDO-03), aunque dos
-- transacciones concurrentes pasen a la vez la verificación del dominio. daterange '[)' = convención de la Estancia.
-- El tiempo de preparación (RN-20) no se expresa aquí: lo garantiza el dominio.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reserva ADD CONSTRAINT ex_reserva_sin_solape
    EXCLUDE USING gist (apartamento_codigo WITH =, daterange(entrada, salida, '[)') WITH &&)
    WHERE (estado IN ('PENDIENTE', 'CONFIRMADA', 'EN_CURSO'));
