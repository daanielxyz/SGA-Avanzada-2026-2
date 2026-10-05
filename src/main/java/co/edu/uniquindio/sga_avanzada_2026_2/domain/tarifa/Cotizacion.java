package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

import java.util.List;

/**
 * Calculado (no se guarda con identidad propia): desglose por noche y total de una estancia. Cotizar no reserva ni
 * retiene noches (COT-01) y no compromete precio hasta crear la reserva (COT-05).
 */
public record Cotizacion(List<LineaCotizacion> desglose, Dinero total) {

    // COT-02 · RN-05: el total es la suma de los subtotales
    public Cotizacion {
        if (desglose == null || desglose.isEmpty() || total == null) {
            throw new ReglaDominioException("La cotización requiere al menos una noche y un total");
        }
        desglose = List.copyOf(desglose);
        Dinero suma = desglose.stream().map(LineaCotizacion::subtotal).reduce(Dinero.CERO, Dinero::sumar);
        if (!suma.equals(total)) {
            throw new ReglaDominioException("El total de la cotización no coincide con la suma del desglose");
        }
    }
}
