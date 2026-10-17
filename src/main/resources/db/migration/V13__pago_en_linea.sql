-- Pago en línea al reservar desde el portal (DEC-69): cuánto se paga (el anticipo o el total) y los minutos que tiene
-- la reserva para pagarse antes de vencer. Los alojamientos que ya existen quedan con el anticipo y 30 minutos.
ALTER TABLE alojamiento ADD COLUMN pago_en_linea VARCHAR(10) DEFAULT 'ANTICIPO' NOT NULL;
ALTER TABLE alojamiento ADD COLUMN plazo_pago_en_linea_min INTEGER DEFAULT 30 NOT NULL;
ALTER TABLE alojamiento ADD CONSTRAINT ck_alojamiento_pago_en_linea CHECK (pago_en_linea IN ('ANTICIPO', 'TOTAL'));
ALTER TABLE alojamiento ADD CONSTRAINT ck_alojamiento_plazo_pago_en_linea CHECK (plazo_pago_en_linea_min > 0);
