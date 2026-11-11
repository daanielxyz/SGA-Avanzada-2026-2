-- BL-12 · DEC-107: cuántas unidades cobra un cargo («dos aguas» en un solo cargo). El valor sigue siendo el total.
-- Los cargos anteriores son de una unidad.
ALTER TABLE folio_cargo ADD COLUMN cantidad INTEGER DEFAULT 1 NOT NULL;
