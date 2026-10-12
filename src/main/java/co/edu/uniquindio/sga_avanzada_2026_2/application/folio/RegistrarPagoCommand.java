package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entrada de {@link RegistrarPago} y de {@link RegistrarDevolucion}: el folio se identifica por su reserva (FOL-02).
 *
 * @param medio nombre del medio de pago, que debe estar habilitado en el alojamiento (MPAG-02)
 * @param fecha cuándo se recibió o se devolvió el dinero; no puede ser futura (PAG-07)
 */
public record RegistrarPagoCommand(String codigoReserva, String medio, BigDecimal monto, LocalDate fecha) {
}
