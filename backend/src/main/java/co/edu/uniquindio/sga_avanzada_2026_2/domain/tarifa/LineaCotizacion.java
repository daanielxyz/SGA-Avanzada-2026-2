package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: línea del desglose por noche: fecha, temporada, tarifa aplicada, ocupantes facturables y subtotal (COT-02).
 * Se congela en la Reserva (RN-22).
 */
public record LineaCotizacion(Noche noche, TemporadaId temporadaId, Dinero tarifa, int ocupantesFacturables,
                              Dinero subtotal) {

    // COT-02
    public LineaCotizacion {
        if (noche == null || temporadaId == null || tarifa == null || subtotal == null) {
            throw new ReglaDominioException("La línea de cotización requiere noche, temporada, tarifa y subtotal");
        }
        if (ocupantesFacturables < 0) {
            throw new ReglaDominioException("Los ocupantes facturables no pueden ser negativos");
        }
    }
}
