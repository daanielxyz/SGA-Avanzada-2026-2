package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import java.util.Objects;

/**
 * Objeto de valor: Tramo de penalidad por cancelación según días de antelación.
 */
public record TramoCancelacion(int antelacionMinDias, Porcentaje retencion) {

    public TramoCancelacion {
        if (antelacionMinDias < 0) {
            throw new IllegalArgumentException("La antelación mínima en días no puede ser negativa");
        }
        Objects.requireNonNull(retencion, "El porcentaje de retención no puede ser nulo");
    }
}
