package co.edu.uniquindio.sga_avanzada_2026_2.domain.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;

import java.util.Optional;

/**
 * Puerto de persistencia del agregado PoliticaCancelacion. Las versiones solo se agregan, nunca se actualizan
 * (POL-04).
 */
public interface PoliticaCancelacionRepository {

    void guardar(PoliticaCancelacion politica);

    /** La versión congelada en una reserva, para cancelar o declarar no-show (RN-13). */
    Optional<PoliticaCancelacion> buscarPorId(PoliticaId id);

    /** La versión más alta del alojamiento (POL-01 · DEC-42), la que congela una reserva nueva. */
    Optional<PoliticaCancelacion> buscarVigente(AlojamientoId alojamientoId);
}
