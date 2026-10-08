package co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: ver las temporadas del alojamiento, incluida la base (CU-33).
 */
@Service
public class ConsultarCalendario {

    private final CalendarioTemporadasRepository calendarios;

    public ConsultarCalendario(CalendarioTemporadasRepository calendarios) {
        this.calendarios = calendarios;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no tiene calendario
     */
    @Transactional(readOnly = true)
    public CalendarioResult ejecutar(String alojamientoId) {
        AlojamientoId id = new AlojamientoId(alojamientoId);
        return calendarios.buscarPorAlojamiento(id)
                .map(CalendarioResult::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("el calendario de temporadas del alojamiento",
                        id.valor()));
    }
}
