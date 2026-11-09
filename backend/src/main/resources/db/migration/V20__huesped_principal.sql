-- DEC-97: el huésped principal del TRA (Resolución 700 de 2021), uno por reserva y mayor de edad. En las reservas que
-- ya existen lo es el ocupante con el documento del titular, que hasta ahora debía alojarse (TIT-01); si no lo hay,
-- el primero del grupo.
ALTER TABLE reserva_ocupante ADD COLUMN principal BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE reserva_ocupante SET principal = TRUE
WHERE EXISTS (SELECT 1 FROM reserva r JOIN titular t ON t.id = r.titular_id
              WHERE r.codigo = reserva_ocupante.reserva_codigo
                AND t.tipo_documento = reserva_ocupante.tipo_documento
                AND t.numero_documento = reserva_ocupante.numero_documento);

UPDATE reserva_ocupante SET principal = TRUE
WHERE orden = 0
  AND NOT EXISTS (SELECT 1 FROM reserva_ocupante otro
                  WHERE otro.reserva_codigo = reserva_ocupante.reserva_codigo AND otro.principal);
