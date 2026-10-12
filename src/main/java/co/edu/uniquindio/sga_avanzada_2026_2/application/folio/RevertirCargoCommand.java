package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

/**
 * Entrada de {@link RevertirCargo}: el cargo mal registrado que se corrige y por qué (CAR-03).
 */
public record RevertirCargoCommand(String codigoReserva, String cargoId, String motivo) {
}
