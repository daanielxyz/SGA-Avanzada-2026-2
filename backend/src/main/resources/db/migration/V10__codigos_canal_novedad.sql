-- Series de los códigos de canal, conflicto de canal, evento de la bitácora y novedad (DEC-52).
CREATE SEQUENCE seq_canal START WITH 1;
CREATE SEQUENCE seq_conflicto START WITH 1;
CREATE SEQUENCE seq_evento START WITH 1;
CREATE SEQUENCE seq_novedad START WITH 1;

-- El administrador lista los canales de su alojamiento (CU-54).
CREATE INDEX ix_canal_alojamiento ON canal (alojamiento_id);
