-- DEC-130: el RNT del alojamiento y el envío del TRA de cada reserva al Ministerio, sin los datos personales enviados.
ALTER TABLE alojamiento ADD COLUMN rnt VARCHAR(20);                         -- opcional hasta encender el TRA

CREATE TABLE envio_tra (
    reserva_codigo   VARCHAR(20)  PRIMARY KEY REFERENCES reserva (codigo),  -- uno por reserva
    estado           VARCHAR(20)  NOT NULL,                                 -- PENDIENTE, ENVIADO o CON_ERROR
    codigo_principal VARCHAR(40),                                           -- el que devolvió el Ministerio
    intentos         INT          NOT NULL,
    ultimo_intento   TIMESTAMP,
    ultimo_error     VARCHAR(500),
    enviado_en       TIMESTAMP,
    version          BIGINT       NOT NULL,
    CHECK ((estado = 'ENVIADO') = (enviado_en IS NOT NULL AND codigo_principal IS NOT NULL)),
    CHECK ((estado = 'CON_ERROR') = (ultimo_error IS NOT NULL))
);

-- Los acompañantes que ya entraron: un reintento no los repite.
CREATE TABLE envio_tra_acompanante (
    reserva_codigo VARCHAR(20) NOT NULL REFERENCES envio_tra (reserva_codigo),
    ocupante_id    VARCHAR(40) NOT NULL,
    PRIMARY KEY (reserva_codigo, ocupante_id)
);

CREATE INDEX ix_envio_tra_estado ON envio_tra (estado);
