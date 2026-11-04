-- Agregado Alojamiento: raíz con sus parámetros de negocio (DEC-25) + servicios adicionales y medios de pago.
-- apartamento.alojamiento_id no lleva FK: entre agregados solo se guarda el id (DEC-14).

CREATE TABLE alojamiento (
    id                           VARCHAR(40)      PRIMARY KEY,
    nombre                       VARCHAR(120)     NOT NULL,
    descripcion                  VARCHAR(2000)    NOT NULL,
    ciudad                       VARCHAR(80)      NOT NULL,
    direccion                    VARCHAR(200)     NOT NULL,
    latitud                      DOUBLE PRECISION NOT NULL CHECK (latitud BETWEEN -90 AND 90),     -- UBI-01
    longitud                     DOUBLE PRECISION NOT NULL CHECK (longitud BETWEEN -180 AND 180),  -- UBI-01
    normas                       VARCHAR(4000),
    -- ParametrosAlojamiento (ALO-03)
    umbral_edad_facturable       INTEGER          NOT NULL CHECK (umbral_edad_facturable >= 0),
    hora_entrada                 TIME             NOT NULL,
    hora_salida                  TIME             NOT NULL,
    tiempo_preparacion_min       INTEGER          NOT NULL CHECK (tiempo_preparacion_min >= 0),   -- TPRE-01
    plazo_confirmacion_min       INTEGER          NOT NULL CHECK (plazo_confirmacion_min > 0),
    hora_limite_no_show          TIME             NOT NULL,
    anticipo_pct                 INTEGER          NOT NULL CHECK (anticipo_pct BETWEEN 0 AND 100), -- PORC-01
    minimo_medios_pago           INTEGER          NOT NULL CHECK (minimo_medios_pago >= 0),       -- ALO-06
    minimo_servicios_adicionales INTEGER          NOT NULL CHECK (minimo_servicios_adicionales >= 0),
    version                      BIGINT           NOT NULL
);

CREATE TABLE alojamiento_servicio_adicional (
    alojamiento_id VARCHAR(40)   NOT NULL REFERENCES alojamiento (id),
    orden          INTEGER       NOT NULL,
    id             VARCHAR(40)   NOT NULL,
    nombre         VARCHAR(120)  NOT NULL,
    genera_cargo   BOOLEAN       NOT NULL,
    valor          NUMERIC(15,0) NOT NULL CHECK (valor >= 0),                                       -- SERV-01
    activo         BOOLEAN       NOT NULL,
    PRIMARY KEY (alojamiento_id, orden),
    UNIQUE (alojamiento_id, id)
);

-- Solo los medios habilitados (MPAG-06). Es un conjunto: deshabilitar borra su fila, los pagos guardan el nombre.
CREATE TABLE alojamiento_medio_pago (
    alojamiento_id VARCHAR(40) NOT NULL REFERENCES alojamiento (id),
    nombre         VARCHAR(40) NOT NULL,
    PRIMARY KEY (alojamiento_id, nombre)
);
