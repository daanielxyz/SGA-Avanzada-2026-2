package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

/**
 * Entrada de {@link RevertirCargoUseCase}: el cargo mal registrado que se corrige, por qué (CAR-03) y quién
 * (DEC-100).
 */
public record RevertirCargoCommand(String codigoReserva, String cargoId, String motivo, String autor) {
}
