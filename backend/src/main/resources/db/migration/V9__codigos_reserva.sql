-- Series de los códigos de reserva, folio y titular (DEC-52). La reserva lleva además el año: RES-2026-00042.
CREATE SEQUENCE seq_titular START WITH 1;
CREATE SEQUENCE seq_folio START WITH 1;
CREATE SEQUENCE seq_reserva START WITH 1;

-- El planificador busca las reservas PENDIENTE que superaron el plazo de confirmación (RN-21).
CREATE INDEX ix_reserva_estado_creada ON reserva (estado, creada_en);
