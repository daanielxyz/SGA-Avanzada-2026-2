-- #10 · DEC-117: el desglose congelado guarda también el nombre de la temporada de cada noche, para que recepción
-- no lea solo su id. Las reservas que ya existen toman el nombre actual de su temporada (hoy no se renombran).
ALTER TABLE reserva_desglose_noche ADD COLUMN temporada_nombre VARCHAR(80);

UPDATE reserva_desglose_noche
SET temporada_nombre = (SELECT t.nombre FROM temporada t WHERE t.id = reserva_desglose_noche.temporada_id);

-- Por si alguna noche apunta a una temporada que ya no está: queda con su id, nunca vacía.
UPDATE reserva_desglose_noche SET temporada_nombre = temporada_id WHERE temporada_nombre IS NULL;

ALTER TABLE reserva_desglose_noche ALTER COLUMN temporada_nombre SET NOT NULL;
