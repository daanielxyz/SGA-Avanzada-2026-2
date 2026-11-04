-- BL-05 · DEC-83: la marca que muestra la web de reservas. Todo opcional: vacío, la web usa sus valores neutros.
ALTER TABLE alojamiento ADD COLUMN marca_color_fondo VARCHAR(7);
ALTER TABLE alojamiento ADD COLUMN marca_color_superficie VARCHAR(7);
ALTER TABLE alojamiento ADD COLUMN marca_color_texto VARCHAR(7);
ALTER TABLE alojamiento ADD COLUMN marca_color_texto_suave VARCHAR(7);
ALTER TABLE alojamiento ADD COLUMN marca_color_borde VARCHAR(7);
ALTER TABLE alojamiento ADD COLUMN marca_color_borde_campo VARCHAR(7);
ALTER TABLE alojamiento ADD COLUMN marca_color_acento VARCHAR(7);
ALTER TABLE alojamiento ADD COLUMN marca_color_texto_sobre_acento VARCHAR(7);
ALTER TABLE alojamiento ADD COLUMN marca_color_peligro VARCHAR(7);
ALTER TABLE alojamiento ADD COLUMN marca_color_exito VARCHAR(7);
ALTER TABLE alojamiento ADD COLUMN marca_fuente_titulos VARCHAR(60);
ALTER TABLE alojamiento ADD COLUMN marca_fuente_texto VARCHAR(60);
ALTER TABLE alojamiento ADD COLUMN marca_radio_pequeno VARCHAR(4);
ALTER TABLE alojamiento ADD COLUMN marca_radio_mediano VARCHAR(4);
ALTER TABLE alojamiento ADD COLUMN marca_radio_grande VARCHAR(4);
ALTER TABLE alojamiento ADD COLUMN marca_logo_url VARCHAR(2000);
ALTER TABLE alojamiento ADD COLUMN marca_portada_url VARCHAR(2000);
