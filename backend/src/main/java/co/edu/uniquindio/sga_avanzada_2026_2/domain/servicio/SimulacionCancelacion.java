package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Saldo;

/**
 * Lo que pasaría si la reserva se cancelara en este momento; no cambia nada (DEC-91).
 *
 * @param retenido lo que se retendría
 * @param saldo    cómo quedaría el folio
 */
public record SimulacionCancelacion(Dinero retenido, Saldo saldo) {
}
