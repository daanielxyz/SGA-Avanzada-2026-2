package co.edu.uniquindio.sga_avanzada_2026_2.domain.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: a partir de antelacionMinDias se retiene el porcentaje indicado (retención + devolución = 100).
 */
public record TramoCancelacion(int antelacionMinDias, Porcentaje retencion) {

    public TramoCancelacion {
        if (antelacionMinDias < 0) {
            throw new ReglaDominioException("La antelación mínima no puede ser negativa");
        }
        if (retencion == null) {
            throw new ReglaDominioException("La retención es obligatoria");
        }
    }
}
