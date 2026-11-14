-- #14 · DEC-120: historial de documentos de ocupantes o del titular corregidos porque se escribieron mal. Va en la
-- reserva, no en el ocupante, porque los ids de los ocupantes pueden cambiar al modificar; por eso guarda el nombre que
-- tenía.
CREATE TABLE reserva_correccion_documento (
    reserva_codigo  VARCHAR(20)  NOT NULL REFERENCES reserva (codigo),
    orden           INTEGER      NOT NULL,
    ocupante        VARCHAR(200) NOT NULL,
    tipo_anterior   VARCHAR(20)  NOT NULL,
    numero_anterior VARCHAR(30)  NOT NULL,
    tipo_nuevo      VARCHAR(20)  NOT NULL,
    numero_nuevo    VARCHAR(30)  NOT NULL,
    motivo          VARCHAR(500) NOT NULL,
    autor           VARCHAR(40)  NOT NULL,
    fecha_hora      TIMESTAMP    NOT NULL,
    titular         BOOLEAN      NOT NULL DEFAULT FALSE,                    -- si fue el documento del titular
    PRIMARY KEY (reserva_codigo, orden)
);
