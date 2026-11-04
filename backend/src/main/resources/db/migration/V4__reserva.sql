-- Agregado Reserva: raíz (con registro y salida, que son 0..1) + ocupantes + desglose congelado por noche.
-- apartamento, titular, canal y política son otros agregados: solo el id, sin relación JPA (DEC-14).

CREATE TABLE reserva (
    codigo                VARCHAR(20)   PRIMARY KEY,
    apartamento_codigo    VARCHAR(20)   NOT NULL,
    titular_id            VARCHAR(40)   NOT NULL,
    entrada               DATE          NOT NULL,
    salida                DATE          NOT NULL,                          -- exclusiva (EST-01)
    estado                VARCHAR(20)   NOT NULL,
    canal_origen          VARCHAR(20)   NOT NULL,
    canal_id              VARCHAR(40),
    id_externo            VARCHAR(80),
    hora_estimada_llegada TIME,                                            -- obligatoria para confirmar (RN-09)
    valor_total           NUMERIC(15,0) NOT NULL CHECK (valor_total >= 0), -- congelado (RN-22)
    politica_version_id   VARCHAR(40)   NOT NULL,                          -- congelada (RN-22)
    creada_en             TIMESTAMP     NOT NULL,                          -- plazo de confirmación (RN-21)
    registro_id           VARCHAR(40),
    registro_fecha_hora   TIMESTAMP,
    registro_autor        VARCHAR(40),
    registro_anulado      BOOLEAN,
    salida_id             VARCHAR(40),
    salida_fecha_hora     TIMESTAMP,
    salida_autor          VARCHAR(40),
    version               BIGINT        NOT NULL,                          -- bloqueo optimista (DEC-17)
    CHECK (salida > entrada),                                              -- RN-03
    CHECK ((canal_origen = 'EXTERNO' AND canal_id IS NOT NULL AND id_externo IS NOT NULL)
        OR (canal_origen <> 'EXTERNO' AND canal_id IS NULL AND id_externo IS NULL)), -- CORI-03
    UNIQUE (canal_id, id_externo)                                          -- RN-19 (los NULL no chocan)
);

CREATE INDEX ix_reserva_apartamento_estado ON reserva (apartamento_codigo, estado);

CREATE TABLE reserva_ocupante (
    reserva_codigo   VARCHAR(20)  NOT NULL REFERENCES reserva (codigo),
    orden            INTEGER      NOT NULL,
    id               VARCHAR(40)  NOT NULL,
    nombre           VARCHAR(120) NOT NULL,
    fecha_nacimiento DATE         NOT NULL,                                -- la edad se calcula (OCU-01)
    tipo_documento   VARCHAR(20),
    numero_documento VARCHAR(30),
    nacionalidad     VARCHAR(60),
    PRIMARY KEY (reserva_codigo, orden),
    UNIQUE (reserva_codigo, id)
);

CREATE TABLE reserva_desglose_noche (
    reserva_codigo        VARCHAR(20)   NOT NULL REFERENCES reserva (codigo),
    orden                 INTEGER       NOT NULL,
    noche                 DATE          NOT NULL,
    temporada_id          VARCHAR(40)   NOT NULL,
    tarifa                NUMERIC(15,0) NOT NULL,
    ocupantes_facturables INTEGER       NOT NULL CHECK (ocupantes_facturables >= 0),
    subtotal              NUMERIC(15,0) NOT NULL,
    PRIMARY KEY (reserva_codigo, orden)
);
