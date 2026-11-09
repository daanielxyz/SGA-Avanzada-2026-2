-- DEC-98: el titular con nombres y apellidos aparte, y la validación de identidad que hace recepción en persona.
-- Los titulares que ya existen conservan su nombre completo en nombres, sin apellidos aparte.
ALTER TABLE titular RENAME COLUMN nombre TO nombres;
ALTER TABLE titular ADD COLUMN primer_apellido VARCHAR(60);
ALTER TABLE titular ADD COLUMN segundo_apellido VARCHAR(60);
ALTER TABLE titular ADD COLUMN validado_por VARCHAR(40);
ALTER TABLE titular ADD COLUMN validado_en TIMESTAMP;
