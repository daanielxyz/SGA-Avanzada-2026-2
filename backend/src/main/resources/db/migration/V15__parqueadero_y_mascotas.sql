-- RP-02 (cupo de parqueadero) y RP-03 (mascotas): parámetros del alojamiento (L-16) y lo que declara cada reserva.
-- Los alojamientos y reservas que ya existen quedan sin parqueadero, sin mascotas y sin vehículos declarados.
ALTER TABLE alojamiento ADD COLUMN cupo_parqueadero INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE alojamiento ADD COLUMN acepta_mascotas BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE alojamiento ADD COLUMN limite_mascotas INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE alojamiento ADD COLUMN cargo_mascota NUMERIC(15, 0) DEFAULT 0 NOT NULL;
ALTER TABLE alojamiento ADD CONSTRAINT ck_alojamiento_parqueadero_mascotas
    CHECK (cupo_parqueadero >= 0 AND limite_mascotas >= 0 AND cargo_mascota >= 0);

ALTER TABLE reserva ADD COLUMN vehiculos INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE reserva ADD COLUMN mascotas INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE reserva ADD CONSTRAINT ck_reserva_vehiculos_mascotas CHECK (vehiculos >= 0 AND mascotas >= 0);
