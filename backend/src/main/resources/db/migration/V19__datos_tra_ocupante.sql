-- BL-07: datos del TRA de cada ocupante. Se toman en la llegada y la salida los exige. Las filas que ya existen
-- conservan su nombre completo en nombres, sin apellidos aparte: recepción los separa al completar el TRA.
ALTER TABLE reserva_ocupante RENAME COLUMN nombre TO nombres;
ALTER TABLE reserva_ocupante ADD COLUMN primer_apellido VARCHAR(60);
ALTER TABLE reserva_ocupante ADD COLUMN segundo_apellido VARCHAR(60);
ALTER TABLE reserva_ocupante ADD COLUMN genero VARCHAR(20);
ALTER TABLE reserva_ocupante ADD COLUMN pais_residencia VARCHAR(60);
ALTER TABLE reserva_ocupante ADD COLUMN ciudad_residencia VARCHAR(80);
ALTER TABLE reserva_ocupante ADD COLUMN ciudad_procedencia VARCHAR(80);
ALTER TABLE reserva_ocupante ADD COLUMN ciudad_destino VARCHAR(80);
ALTER TABLE reserva_ocupante ADD COLUMN motivo_viaje VARCHAR(30);
