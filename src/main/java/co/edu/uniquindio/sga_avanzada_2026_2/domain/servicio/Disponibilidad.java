package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Calculado (no se guarda): resultado de verificar la disponibilidad de un apartamento. Vale solo para el instante
 * del cálculo; consultarlo no reserva ni bloquea nada (DISP-06).
 *
 * @param motivo por qué no está disponible; {@code null} si lo está
 */
public record Disponibilidad(ApartamentoId apartamentoId, Estancia estancia, boolean disponible, String motivo) {

    public Disponibilidad {
        if (apartamentoId == null || estancia == null) {
            throw new ReglaDominioException("La disponibilidad requiere apartamento y estancia");
        }
        if (disponible != (motivo == null)) {
            throw new ReglaDominioException("Solo una disponibilidad negativa lleva motivo");
        }
    }

    static Disponibilidad libre(ApartamentoId apartamentoId, Estancia estancia) {
        return new Disponibilidad(apartamentoId, estancia, true, null);
    }

    static Disponibilidad ocupada(ApartamentoId apartamentoId, Estancia estancia, String motivo) {
        return new Disponibilidad(apartamentoId, estancia, false, motivo);
    }

    /**
     * Para crear o modificar una reserva: rechaza si el apartamento no está disponible (RN-01 · RN-07 · RN-20).
     *
     * @throws ReglaDominioException con el motivo, si no está disponible
     */
    public void exigir() {
        if (!disponible) {
            throw new ReglaDominioException(motivo);
        }
    }
}
