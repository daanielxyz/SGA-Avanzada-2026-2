package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDateTime;

/**
 * Quién marcó la nota como atendida y cuándo (BL-08 · DEC-105).
 */
public record AtencionNota(UsuarioId autor, LocalDateTime fechaHora) {

    public AtencionNota {
        if (autor == null || fechaHora == null) {
            throw new ReglaDominioException("La atención de la nota requiere autor y fecha");
        }
    }
}
