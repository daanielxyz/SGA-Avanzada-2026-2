package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Salida de {@link ReservarDesdePortalUseCase}: la reserva PENDIENTE y lo que falta para que quede confirmada (DEC-69).
 *
 * @param montoAPagar  lo que se cobra en línea (el anticipo o el total, según el alojamiento)
 * @param pagarAntesDe si no se paga antes, la reserva vence y libera sus noches
 * @param cargosAdicionales lo que el folio suma al valor de la estancia, hoy las mascotas (RP-03); no entra en el
 *                     monto en línea, que sale solo del valor de la estancia
 */
public record ReservaPortalResult(ReservaResult reserva, BigDecimal montoAPagar, LocalDateTime pagarAntesDe,
                                  BigDecimal cargosAdicionales) {
}
