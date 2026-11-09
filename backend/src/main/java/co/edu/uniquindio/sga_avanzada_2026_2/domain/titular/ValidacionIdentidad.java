package co.edu.uniquindio.sga_avanzada_2026_2.domain.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;

/**
 * VO: recepción confirmó en persona, con el documento, la identidad del titular (DEC-98).
 *
 * @param autor     quien la validó
 * @param fechaHora cuándo, en Colombia
 */
public record ValidacionIdentidad(UsuarioId autor, LocalDateTime fechaHora) {

    public ValidacionIdentidad {
        if (autor == null || fechaHora == null) {
            throw new ReglaDominioException("La validación de identidad requiere autor y fecha");
        }
    }
}
