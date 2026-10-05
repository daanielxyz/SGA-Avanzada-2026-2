-- Mínimo configurable de temporadas específicas para activar apartamentos (TEM-04 · DEC-28).
-- No se edita V2: una migración aplicada nunca se modifica, se agrega otra.
ALTER TABLE alojamiento ADD COLUMN minimo_temporadas INTEGER DEFAULT 2 NOT NULL;
ALTER TABLE alojamiento ADD CONSTRAINT ck_alojamiento_minimo_temporadas CHECK (minimo_temporadas >= 0);

-- Agregado CalendarioTemporadas: uno por alojamiento (DEC-28) + sus temporadas.
CREATE TABLE calendario_temporadas (
    alojamiento_id VARCHAR(40) PRIMARY KEY,
    version        BIGINT      NOT NULL
);

CREATE TABLE temporada (
    alojamiento_id         VARCHAR(40) NOT NULL REFERENCES calendario_temporadas (alojamiento_id),
    orden                  INTEGER     NOT NULL,
    id                     VARCHAR(40) NOT NULL UNIQUE,  -- Tarifa la referencia por id
    nombre                 VARCHAR(80) NOT NULL,
    fecha_inicio           DATE,                         -- null en la base (TEM-09)
    fecha_fin              DATE,                         -- inclusiva (TEM-01)
    es_base                BOOLEAN     NOT NULL,
    estancia_minima_noches INTEGER     NOT NULL CHECK (estancia_minima_noches >= 0),  -- TEM-07
    activa                 BOOLEAN     NOT NULL,
    PRIMARY KEY (alojamiento_id, orden),
    CHECK (es_base OR fecha_fin >= fecha_inicio)                                       -- TEM-01
);

-- Agregado Tarifa: una fila por versión, nunca se actualiza (TAR-05).
CREATE TABLE tarifa (
    id                 VARCHAR(40)   PRIMARY KEY,
    apartamento_codigo VARCHAR(20)   NOT NULL,
    temporada_id       VARCHAR(40)   NOT NULL,
    valor_por_ocupante NUMERIC(15,0) NOT NULL CHECK (valor_por_ocupante > 0),  -- TAR-02
    version            INTEGER       NOT NULL CHECK (version >= 1),
    vigente_desde      DATE          NOT NULL,
    UNIQUE (apartamento_codigo, temporada_id, version)                         -- TAR-05
);
