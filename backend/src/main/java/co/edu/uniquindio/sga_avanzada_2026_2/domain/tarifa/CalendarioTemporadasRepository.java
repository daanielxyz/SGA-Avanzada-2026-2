package co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;

import java.util.Optional;

/**
 * Puerto de persistencia del agregado CalendarioTemporadas (uno por alojamiento), con todas sus temporadas.
 */
public interface CalendarioTemporadasRepository {

    void guardar(CalendarioTemporadas calendario);

    Optional<CalendarioTemporadas> buscarPorAlojamiento(AlojamientoId alojamientoId);
}
