-- DEC-93: la clave de una reserva del portal. Se entrega una sola vez, al reservar, y solo la tiene el navegador
-- que reservó: con ella se paga sin cuenta y se libera la reserva al volver desde el pago. Como codigo_acceso, vive
-- fuera del dominio y solo guarda la huella, nunca la clave.
CREATE TABLE clave_reserva (
    reserva_codigo VARCHAR(40) PRIMARY KEY,                                     -- sin FK: es de seguridad
    huella         VARCHAR(64) NOT NULL,                                        -- SHA-256 en hexadecimal
    creada_en      TIMESTAMP   NOT NULL
);

-- DEC-93: cuántas mascotas admite cada apartamento por reserva; vacío, las del alojamiento.
ALTER TABLE apartamento ADD COLUMN limite_mascotas INT;
ALTER TABLE apartamento ADD CONSTRAINT ck_apartamento_limite_mascotas CHECK (limite_mascotas >= 0);
