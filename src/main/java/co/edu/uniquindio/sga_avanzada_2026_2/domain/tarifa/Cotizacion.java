package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;

import java.util.List;

/**
 * Calculado (no se guarda): desglose por noche y total de una estancia.
 */
public record Cotizacion(List<LineaCotizacion> desglose, Dinero total) {

    public Cotizacion {
        desglose = List.copyOf(desglose);
    }
}
