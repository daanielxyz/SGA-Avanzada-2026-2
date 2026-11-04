package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

/**
 * Entrada de {@link RevertirPagoUseCase}: el abono mal registrado que se corrige (PAG-03).
 */
public record RevertirPagoCommand(String codigoReserva, String pagoId) {
}
