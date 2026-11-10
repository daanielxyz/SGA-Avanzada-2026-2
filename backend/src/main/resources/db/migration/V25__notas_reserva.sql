-- BL-08 · DEC-105: notas de la reserva para el siguiente turno. No se editan ni se borran: se marcan como atendidas.
CREATE TABLE nota_reserva (
    id             VARCHAR(40)  PRIMARY KEY,
    reserva_codigo VARCHAR(40)  NOT NULL,
    texto          VARCHAR(500) NOT NULL,
    autor          VARCHAR(40)  NOT NULL,
    escrita_en     TIMESTAMP    NOT NULL,
    atendida_por   VARCHAR(40),                -- nulas las dos mientras está pendiente
    atendida_en    TIMESTAMP,
    version        BIGINT       NOT NULL
);

CREATE INDEX ix_nota_reserva_reserva ON nota_reserva (reserva_codigo);

CREATE SEQUENCE seq_nota START WITH 1;
