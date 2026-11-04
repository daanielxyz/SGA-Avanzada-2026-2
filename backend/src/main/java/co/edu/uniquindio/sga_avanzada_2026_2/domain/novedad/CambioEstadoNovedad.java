package co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;

/**
 * VO: un paso del ciclo de la novedad con su autor y fecha (NOV-04). El primero es el registro (ABIERTA).
 */
public record CambioEstadoNovedad(EstadoNovedad estado, UsuarioId autor, LocalDateTime fechaHora) {

    // NOV-04
    public CambioEstadoNovedad {
        if (estado == null || autor == null || fechaHora == null) {
            throw new ReglaDominioException("Cada paso de la novedad requiere estado, autor y fecha");
        }
    }
}
