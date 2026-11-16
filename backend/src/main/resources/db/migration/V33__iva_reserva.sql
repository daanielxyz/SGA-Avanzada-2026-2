-- DEC-134: el IVA se congela en la reserva desde que se crea. Porcentaje 0 = la reserva no lleva IVA (las de antes de
-- este cambio, las de un alojamiento que no lo cobra y las de canales externos); exenta = decide un pasaporte.
ALTER TABLE reserva ADD COLUMN iva_porcentaje INTEGER DEFAULT 0 NOT NULL CHECK (iva_porcentaje BETWEEN 0 AND 100);
ALTER TABLE reserva ADD COLUMN iva_exento BOOLEAN DEFAULT FALSE NOT NULL;
