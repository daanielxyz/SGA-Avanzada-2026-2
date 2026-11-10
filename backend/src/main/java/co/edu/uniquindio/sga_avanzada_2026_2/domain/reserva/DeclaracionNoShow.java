package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;

/**
 * Quién declaró que el titular no se presentó y cuándo (BL-25 · DEC-103).
 *
 * @param fechaHora el momento real en que se declaró, no la hora límite
 */
public record DeclaracionNoShow(UsuarioId autor, LocalDateTime fechaHora) {

    public DeclaracionNoShow {
        if (autor == null || fechaHora == null) {
            throw new ReglaDominioException("La declaración del no-show requiere autor, fecha y hora");
        }
    }
}
