package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: coordenadas del alojamiento (UBI-01). Pertenece al alojamiento, no al apartamento (UBI-02); se reemplaza
 * completa (UBI-03).
 */
public record Ubicacion(double latitud, double longitud) {

    // UBI-01
    public Ubicacion {
        if (latitud < -90 || latitud > 90) {
            throw new ReglaDominioException("Latitud fuera de rango [-90, 90]: " + latitud);
        }
        if (longitud < -180 || longitud > 180) {
            throw new ReglaDominioException("Longitud fuera de rango [-180, 180]: " + longitud);
        }
    }
}
