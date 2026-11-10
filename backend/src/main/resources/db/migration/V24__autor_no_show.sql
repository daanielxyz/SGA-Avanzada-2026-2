-- BL-25 · DEC-103: quién declaró el no-show y cuándo.
-- Los no-show anteriores a esta migración quedan sin estos datos: no se guardaban.
ALTER TABLE reserva ADD COLUMN no_show_autor VARCHAR(40);
ALTER TABLE reserva ADD COLUMN no_show_fecha_hora TIMESTAMP;
