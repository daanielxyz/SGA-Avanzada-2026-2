-- #17 · DEC-113: por qué el administrador declaró el apartamento fuera de servicio. Solo mientras siga así;
-- al volver a la ruta de preparación queda vacío.
ALTER TABLE apartamento ADD COLUMN motivo_fuera_de_servicio VARCHAR(500);
