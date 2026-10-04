package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;

/**
 * VO: autorización para cerrar el folio con saldo distinto de cero.
 */
public record AutorizacionCierre(UsuarioId autor, String motivo, LocalDateTime fechaHora) {

    // RN-17
    public AutorizacionCierre {
        if (autor == null || fechaHora == null) {
            throw new ReglaDominioException("La autorización requiere autor y fecha");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDominioException("La autorización requiere motivo");
        }
    }
}
