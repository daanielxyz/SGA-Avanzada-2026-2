-- DEC-133: el IVA de la estancia es configuración del alojamiento. Viene apagado, con el 19 % listo para encenderlo.
ALTER TABLE alojamiento ADD COLUMN cobra_iva BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE alojamiento ADD COLUMN porcentaje_iva INTEGER DEFAULT 19 NOT NULL CHECK (porcentaje_iva BETWEEN 0 AND 100);
