-- Mínimo configurable de tramos de la política de cancelación (POL-01 · DEC-41).
ALTER TABLE alojamiento ADD COLUMN minimo_tramos_cancelacion INTEGER DEFAULT 2 NOT NULL;
ALTER TABLE alojamiento ADD CONSTRAINT ck_alojamiento_minimo_tramos CHECK (minimo_tramos_cancelacion >= 1);

-- Agregado PoliticaCancelacion: una fila por versión, nunca se actualiza (POL-04). La vigente es la de versión
-- más alta del alojamiento (DEC-42). Cada penalización es un porcentaje o un monto fijo sobre una base (DEC-41).
CREATE TABLE politica_cancelacion (
    id                    VARCHAR(40)   PRIMARY KEY,
    alojamiento_id        VARCHAR(40)   NOT NULL,
    version               INTEGER       NOT NULL CHECK (version >= 1),
    no_show_base          VARCHAR(20)   NOT NULL,                                    -- POL-06
    no_show_porcentaje    INTEGER       CHECK (no_show_porcentaje BETWEEN 0 AND 100),
    no_show_monto_fijo    NUMERIC(15,0) CHECK (no_show_monto_fijo >= 0),
    UNIQUE (alojamiento_id, version),
    CHECK ((no_show_porcentaje IS NULL) <> (no_show_monto_fijo IS NULL))
);

CREATE TABLE politica_tramo (
    politica_id          VARCHAR(40)   NOT NULL REFERENCES politica_cancelacion (id),
    orden                INTEGER       NOT NULL,
    antelacion_min_horas INTEGER       NOT NULL CHECK (antelacion_min_horas >= 0),   -- TRAM-01
    base                 VARCHAR(20)   NOT NULL,
    porcentaje           INTEGER       CHECK (porcentaje BETWEEN 0 AND 100),         -- POL-03
    monto_fijo           NUMERIC(15,0) CHECK (monto_fijo >= 0),
    PRIMARY KEY (politica_id, orden),
    UNIQUE (politica_id, antelacion_min_horas),                                      -- POL-02
    CHECK ((porcentaje IS NULL) <> (monto_fijo IS NULL))
);

-- Agregado Folio: uno por reserva (FOL-02). Cargos y pagos solo se agregan (RN-16); el saldo no se guarda (SLD-01).
CREATE TABLE folio (
    id                      VARCHAR(40)  PRIMARY KEY,
    reserva_codigo          VARCHAR(20)  NOT NULL UNIQUE,                            -- FOL-02
    cerrado                 BOOLEAN      NOT NULL,
    autorizacion_autor      VARCHAR(40),                                             -- RN-17
    autorizacion_motivo     VARCHAR(500),
    autorizacion_fecha_hora TIMESTAMP,
    version                 BIGINT       NOT NULL,                                   -- bloqueo optimista
    CHECK (cerrado OR autorizacion_autor IS NULL)
);

CREATE TABLE folio_cargo (
    folio_id  VARCHAR(40)   NOT NULL REFERENCES folio (id),
    orden     INTEGER       NOT NULL,
    id        VARCHAR(40)   NOT NULL,
    tipo      VARCHAR(30)   NOT NULL,
    concepto  VARCHAR(200)  NOT NULL,
    valor     NUMERIC(15,0) NOT NULL CHECK (valor > 0),                              -- CAR-06 · DEC-20
    sentido   VARCHAR(12),                                                           -- solo AJUSTE
    fecha     DATE          NOT NULL,
    corrige_a VARCHAR(40),                                                           -- CAR-03
    PRIMARY KEY (folio_id, orden),
    UNIQUE (folio_id, id),
    CHECK ((tipo = 'AJUSTE') = (sentido IS NOT NULL))
);

CREATE TABLE folio_pago (
    folio_id   VARCHAR(40)   NOT NULL REFERENCES folio (id),
    orden      INTEGER       NOT NULL,
    id         VARCHAR(40)   NOT NULL,
    medio      VARCHAR(40)   NOT NULL,                                               -- PAG-01 · MPAG-03
    monto      NUMERIC(15,0) NOT NULL CHECK (monto > 0),                             -- PAG-02
    tipo       VARCHAR(10)   NOT NULL,
    fecha      DATE          NOT NULL,
    reversa_de VARCHAR(40),                                                          -- TPAG-02
    PRIMARY KEY (folio_id, orden),
    UNIQUE (folio_id, id)
);
