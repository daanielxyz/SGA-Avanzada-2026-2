-- DEC-141: el alojamiento decide si sus tarifas ya traen el IVA o se les suma; la reserva congela esa decisión. Lo
-- que ya existe sigue como estaba: el IVA se suma.
ALTER TABLE alojamiento ADD COLUMN iva_incluido BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE reserva ADD COLUMN iva_incluido BOOLEAN DEFAULT FALSE NOT NULL;
