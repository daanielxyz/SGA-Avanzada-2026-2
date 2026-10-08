package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: ver los datos, horarios, normas y servicios del alojamiento (CU-50).
 */
@Service
public class ConsultarAlojamiento {

    private final AlojamientoRepository alojamientos;

    public ConsultarAlojamiento(AlojamientoRepository alojamientos) {
        this.alojamientos = alojamientos;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe
     */
    @Transactional(readOnly = true)
    public AlojamientoResult ejecutar(String alojamientoId) {
        AlojamientoId id = new AlojamientoId(alojamientoId);
        return alojamientos.buscarPorId(id)
                .map(AlojamientoResult::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", id.valor()));
    }
}
