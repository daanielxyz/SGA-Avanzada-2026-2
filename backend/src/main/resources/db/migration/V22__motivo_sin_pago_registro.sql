-- BL-22: por qué un huésped entró sin haber pagado la estancia y sus extras. El autor es el del registro.
ALTER TABLE reserva_registro ADD COLUMN motivo_sin_pago VARCHAR(500);
