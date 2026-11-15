package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador escribe el Registro Nacional de Turismo del alojamiento, con el que se envía el TRA
 * (DEC-130).
 */
@Service
public class RegistrarRntUseCase {

    private final AlojamientoRepository alojamientos;

    public RegistrarRntUseCase(AlojamientoRepository alojamientos) {
        this.alojamientos = alojamientos;
    }

    /**
     * @param rnt vacío o null lo borra
     * @throws RecursoNoEncontradoException si el alojamiento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si no son solo dígitos
     */
    @Transactional
    public AlojamientoResult ejecutar(String alojamientoId, String rnt) {
        AlojamientoId id = new AlojamientoId(alojamientoId);
        Alojamiento alojamiento = alojamientos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", id.valor()));

        alojamiento.registrarRnt(rnt);

        alojamientos.guardar(alojamiento);
        return AlojamientoResult.de(alojamiento);
    }
}
