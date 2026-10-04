package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;

/**
 * VO: línea del desglose por noche (RN-05). Se congela en la Reserva.
 */
public record LineaCotizacion(Noche noche, TemporadaId temporadaId, Dinero tarifa, int ocupantesFacturables,
                              Dinero subtotal) {
}
