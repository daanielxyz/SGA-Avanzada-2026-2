package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ResultadoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.util.Optional;

/**
 * Calculado (no se guarda): resultado de integrar una reserva de un canal externo. EXITOSO trae la reserva nueva,
 * DUPLICADO la que ya existía (RN-19) y CONFLICTO el conflicto registrado (RN-18). El caso de uso guarda lo que venga
 * y lo anota en la bitácora (BIT-03) sin decidir nada.
 */
public record IntegracionExterna(ResultadoEvento resultado, Reserva reserva, ConflictoCanal conflicto) {

    public IntegracionExterna {
        boolean valido = switch (resultado) {
            case EXITOSO, DUPLICADO -> reserva != null && conflicto == null;
            case CONFLICTO -> reserva == null && conflicto != null;
            case RECHAZADO, ERROR -> false;
        };
        if (!valido) {
            throw new ReglaDominioException("Resultado de integración incoherente: " + resultado);
        }
    }

    /** La reserva que hay que guardar (y abrirle folio, DEC-44): solo si se creó ahora. */
    public Optional<Reserva> reservaCreada() {
        return resultado == ResultadoEvento.EXITOSO ? Optional.of(reserva) : Optional.empty();
    }

    public Optional<ConflictoCanal> conflictoRegistrado() {
        return Optional.ofNullable(conflicto);
    }

    /** Detalle para la bitácora: el código de la reserva o el motivo del conflicto (BIT-01). */
    public String detalle() {
        return conflicto != null ? conflicto.motivo() : reserva.codigo().valor();
    }
}
