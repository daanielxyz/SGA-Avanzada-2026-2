package co.edu.uniquindio.sga_avanzada_2026_2.domain.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: si se cancela con al menos {@code antelacionMinHoras} de anticipación a la hora de entrada, se aplica esta
 * penalización (TRAM-01). Las horas reemplazan los días del Excel para admitir políticas como «gratis hasta 48 h
 * antes» (DEC-41).
 */
public record TramoCancelacion(int antelacionMinHoras, Penalizacion penalizacion) {

    // TRAM-01
    public TramoCancelacion {
        if (antelacionMinHoras < 0) {
            throw new ReglaDominioException("La antelación mínima no puede ser negativa");
        }
        if (penalizacion == null) {
            throw new ReglaDominioException("El tramo requiere su penalización");
        }
    }
}
