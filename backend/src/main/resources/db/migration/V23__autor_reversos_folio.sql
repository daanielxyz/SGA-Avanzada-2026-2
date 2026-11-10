-- DEC-100: quién corrigió un cargo y quién hizo el reverso de un pago o una devolución.
-- Los movimientos anteriores a esta migración quedan sin autor: el dato no existía.
ALTER TABLE folio_cargo ADD COLUMN autor VARCHAR(40);
ALTER TABLE folio_pago ADD COLUMN autor VARCHAR(40);
