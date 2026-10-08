-- Agregado Canal: la credencial de un canal externo nunca se guarda aquí, solo su referencia en la configuración
-- externa (CAN-02 · DEC-47).
CREATE TABLE canal (
    id                    VARCHAR(40)  PRIMARY KEY,
    alojamiento_id        VARCHAR(40)  NOT NULL,                                -- DEC-26
    nombre                VARCHAR(80)  NOT NULL,
    tipo                  VARCHAR(20)  NOT NULL,
    referencia_credencial VARCHAR(120),                                         -- solo EXTERNO
    activo                BOOLEAN      NOT NULL,                                -- CAN-05
    version               BIGINT       NOT NULL,
    CHECK ((tipo = 'EXTERNO') = (referencia_credencial IS NOT NULL))            -- CAN-01
);

-- Agregado ConflictoCanal: evidencia de auditoría, nunca se elimina (CONF-05).
CREATE TABLE conflicto_canal (
    id                 VARCHAR(40)  PRIMARY KEY,
    canal_id           VARCHAR(40)  NOT NULL,
    id_externo         VARCHAR(80)  NOT NULL,
    apartamento_codigo VARCHAR(20)  NOT NULL,
    entrada            DATE         NOT NULL,                                   -- estancia solicitada (CONF-03)
    salida             DATE         NOT NULL,
    detectado_en       TIMESTAMP    NOT NULL,
    motivo             VARCHAR(500) NOT NULL,
    estado             VARCHAR(20)  NOT NULL,                                   -- CONF-04
    resuelto_por       VARCHAR(40),
    resuelto_en        TIMESTAMP,
    decision           VARCHAR(500),
    version            BIGINT       NOT NULL,
    CHECK (salida > entrada),
    CHECK ((estado = 'RESUELTO') = (resuelto_por IS NOT NULL AND resuelto_en IS NOT NULL AND decision IS NOT NULL))
);

CREATE INDEX ix_conflicto_canal_estado ON conflicto_canal (estado);

-- Agregado EventoCanal: bitácora de solo escritura, sin versión ni actualizaciones (BIT-02).
CREATE TABLE evento_canal (
    id         VARCHAR(40)    PRIMARY KEY,
    canal_id   VARCHAR(40)    NOT NULL,
    operacion  VARCHAR(40)    NOT NULL,
    sentido    VARCHAR(10)    NOT NULL,
    fecha_hora TIMESTAMP      NOT NULL,
    carga_util VARCHAR(10000) NOT NULL,                                         -- nunca credenciales (BIT-04)
    resultado  VARCHAR(20)    NOT NULL,                                         -- BIT-03
    detalle    VARCHAR(500)
);

CREATE INDEX ix_evento_canal_canal_fecha ON evento_canal (canal_id, fecha_hora);

-- Agregado Titular: responsable de la reserva y del pago (TIT-02). Los datos del TRA son de cada ocupante (DEC-49).
CREATE TABLE titular (
    id               VARCHAR(40)  PRIMARY KEY,
    alojamiento_id   VARCHAR(40)  NOT NULL,                                     -- DEC-26
    nombre           VARCHAR(120) NOT NULL,
    tipo_documento   VARCHAR(20)  NOT NULL,
    numero_documento VARCHAR(30)  NOT NULL,
    correo           VARCHAR(150),
    telefono         VARCHAR(30),
    version          BIGINT       NOT NULL,
    UNIQUE (alojamiento_id, tipo_documento, numero_documento),                 -- TIT-05: una identidad por documento
    CHECK (correo IS NOT NULL OR telefono IS NOT NULL)                         -- DEC-49
);

-- Agregado Novedad + su historial de pasos con autor y fecha (NOV-04 · DEC-50). No se elimina (NOV-06).
CREATE TABLE novedad (
    id                 VARCHAR(40)   PRIMARY KEY,
    apartamento_codigo VARCHAR(20)   NOT NULL,
    descripcion        VARCHAR(2000) NOT NULL,
    gravedad           VARCHAR(20)   NOT NULL,                                  -- NOV-02
    version            BIGINT        NOT NULL
);

CREATE INDEX ix_novedad_apartamento ON novedad (apartamento_codigo);

CREATE TABLE novedad_cambio (
    novedad_id VARCHAR(40) NOT NULL REFERENCES novedad (id),
    orden      INTEGER     NOT NULL,
    estado     VARCHAR(20) NOT NULL,
    autor      VARCHAR(40) NOT NULL,
    fecha_hora TIMESTAMP   NOT NULL,
    PRIMARY KEY (novedad_id, orden)
);
