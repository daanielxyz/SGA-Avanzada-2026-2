package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Salida de {@link ReservarDesdePortal}: la reserva PENDIENTE y lo que falta para que quede confirmada (DEC-69).
 *
 * @param montoAPagar  lo que se cobra en línea (el anticipo o el total, según el alojamiento)
 * @param pagarAntesDe si no se paga antes, la reserva vence y libera sus noches
 */
public record ReservaPortalResult(ReservaResult reserva, BigDecimal montoAPagar, LocalDateTime pagarAntesDe) {
}
