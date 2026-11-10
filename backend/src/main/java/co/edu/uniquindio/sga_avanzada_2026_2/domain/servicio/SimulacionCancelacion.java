package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Saldo;

/**
 * Lo que pasaría si la reserva se cancelara (DEC-91) o se declarara no-show (DEC-102) en este momento; no cambia nada.
 *
 * @param retenido lo que se retendría
 * @param saldo    cómo quedaría el folio
 */
public record SimulacionCancelacion(Dinero retenido, Saldo saldo) {
}
