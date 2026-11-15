-- DEC-129: el conflicto de canal guarda a quién llamar (el titular que mandó el canal con la reserva rechazada).
-- Los conflictos de antes quedan sin contacto (null); el documento no se guarda aquí (queda en la bitácora).
ALTER TABLE conflicto_canal ADD COLUMN titular_nombre   VARCHAR(120);
ALTER TABLE conflicto_canal ADD COLUMN titular_telefono VARCHAR(30);
ALTER TABLE conflicto_canal ADD COLUMN titular_correo   VARCHAR(150);
