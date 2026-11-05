package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PoliticaResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Salida de {@link ConsultarPagoPendienteUseCase}: lo que falta para confirmar una reserva en línea (DEC-91).
 *
 * @param montoAPagar  lo que se cobraría en línea
 * @param pagarAntesDe si no se paga antes, la reserva vence y libera sus noches
 * @param condiciones  la política congelada en la reserva: la que se le aplica, no la vigente (RN-13)
 */
public record PagoPendienteResult(BigDecimal montoAPagar, LocalDateTime pagarAntesDe, PoliticaResult condiciones) {
}
