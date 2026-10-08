-- Historial de registros de llegada (REG-06 · DEC-45): un registro equivocado se anula y se reemplaza por una
-- corrección, así que una reserva puede tener varios. Reemplaza las columnas registro_* de V4 (ajusta DEC-40).
-- No se edita V4: una migración aplicada nunca se modifica, se agrega otra.
CREATE TABLE reserva_registro (
    reserva_codigo    VARCHAR(20)  NOT NULL REFERENCES reserva (codigo),
    orden             INTEGER      NOT NULL,
    id                VARCHAR(40)  NOT NULL,
    fecha_hora        TIMESTAMP    NOT NULL,                                     -- REG-05
    autor             VARCHAR(40)  NOT NULL,
    anulado           BOOLEAN      NOT NULL,
    motivo_correccion VARCHAR(500),                                              -- solo en una corrección
    PRIMARY KEY (reserva_codigo, orden),
    UNIQUE (reserva_codigo, id)
);

-- Conserva los registros que ya existían.
INSERT INTO reserva_registro (reserva_codigo, orden, id, fecha_hora, autor, anulado, motivo_correccion)
SELECT codigo, 0, registro_id, registro_fecha_hora, registro_autor, registro_anulado, NULL
FROM reserva
WHERE registro_id IS NOT NULL;

ALTER TABLE reserva DROP COLUMN registro_id;
ALTER TABLE reserva DROP COLUMN registro_fecha_hora;
ALTER TABLE reserva DROP COLUMN registro_autor;
ALTER TABLE reserva DROP COLUMN registro_anulado;
