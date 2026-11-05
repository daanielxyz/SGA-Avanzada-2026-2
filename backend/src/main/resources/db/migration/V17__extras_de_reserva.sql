-- DEC-89: el parqueadero se cobra por noche o por estadía, con precios distintos para carro y moto, y la reserva
-- congela lo que suman el parqueadero y las mascotas. Los alojamientos que ya existen quedan con el parqueadero gratis.
ALTER TABLE alojamiento ADD COLUMN cobro_parqueadero VARCHAR(12) DEFAULT 'POR_NOCHE' NOT NULL;
ALTER TABLE alojamiento ADD COLUMN cargo_carro NUMERIC(15, 0) DEFAULT 0 NOT NULL;
ALTER TABLE alojamiento ADD COLUMN cargo_moto NUMERIC(15, 0) DEFAULT 0 NOT NULL;
ALTER TABLE alojamiento ADD CONSTRAINT ck_alojamiento_cobro_parqueadero
    CHECK (cobro_parqueadero IN ('POR_NOCHE', 'POR_ESTADIA') AND cargo_carro >= 0 AND cargo_moto >= 0);

-- Los vehículos que ya se declararon pasan a ser carros
ALTER TABLE reserva DROP CONSTRAINT ck_reserva_vehiculos_mascotas;
ALTER TABLE reserva RENAME COLUMN vehiculos TO carros;
ALTER TABLE reserva ADD COLUMN motos INTEGER DEFAULT 0 NOT NULL;
ALTER TABLE reserva ADD COLUMN extras_parqueadero NUMERIC(15, 0) DEFAULT 0 NOT NULL;
ALTER TABLE reserva ADD COLUMN extras_mascotas NUMERIC(15, 0) DEFAULT 0 NOT NULL;
ALTER TABLE reserva ADD CONSTRAINT ck_reserva_extras
    CHECK (carros >= 0 AND motos >= 0 AND mascotas >= 0 AND extras_parqueadero >= 0 AND extras_mascotas >= 0);

-- Las mascotas ya cobradas estaban solo en el folio: se copian para que la reserva sepa cuánto anular si se cancela
UPDATE reserva SET extras_mascotas = COALESCE((
    SELECT SUM(c.valor)
    FROM folio f JOIN folio_cargo c ON c.folio_id = f.id
    WHERE f.reserva_codigo = reserva.codigo AND c.tipo = 'SERVICIO_ADICIONAL' AND c.concepto LIKE 'Mascotas (%'), 0);
