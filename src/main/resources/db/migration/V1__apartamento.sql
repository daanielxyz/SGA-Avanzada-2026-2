-- Agregado Apartamento: raíz + hijos (imágenes, características, bloqueos).
-- alojamiento_id es referencia a otro agregado: solo el id, sin relación JPA (DEC-14).

CREATE TABLE apartamento (
    codigo           VARCHAR(20)   PRIMARY KEY,
    alojamiento_id   VARCHAR(40)   NOT NULL,
    nombre           VARCHAR(120)  NOT NULL,
    descripcion      VARCHAR(2000),
    capacidad        INTEGER       NOT NULL CHECK (capacidad >= 1),   -- CAP-01
    dormitorios      INTEGER       NOT NULL CHECK (dormitorios >= 1), -- DOR-02
    estado_operativo VARCHAR(30)   NOT NULL,
    activo           BOOLEAN       NOT NULL,
    version          BIGINT        NOT NULL                           -- bloqueo optimista (DEC-17)
);

CREATE TABLE apartamento_imagen (
    apartamento_codigo VARCHAR(20)  NOT NULL REFERENCES apartamento (codigo),
    orden              INTEGER      NOT NULL,
    url                VARCHAR(500) NOT NULL,
    principal          BOOLEAN      NOT NULL,
    PRIMARY KEY (apartamento_codigo, orden)
);

CREATE TABLE apartamento_caracteristica (
    apartamento_codigo VARCHAR(20)  NOT NULL REFERENCES apartamento (codigo),
    orden              INTEGER      NOT NULL,
    nombre             VARCHAR(100) NOT NULL,
    PRIMARY KEY (apartamento_codigo, orden)
);

CREATE TABLE bloqueo (
    apartamento_codigo VARCHAR(20)  NOT NULL REFERENCES apartamento (codigo),
    orden              INTEGER      NOT NULL,
    id                 VARCHAR(40)  NOT NULL,
    fecha_inicio       DATE         NOT NULL,
    fecha_fin          DATE         NOT NULL,
    motivo             VARCHAR(500) NOT NULL,
    vigente            BOOLEAN      NOT NULL,
    PRIMARY KEY (apartamento_codigo, orden),
    UNIQUE (apartamento_codigo, id),
    CHECK (fecha_fin > fecha_inicio)                                  -- BLO-02
);
