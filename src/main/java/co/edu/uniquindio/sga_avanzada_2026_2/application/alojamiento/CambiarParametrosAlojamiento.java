package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador cambia los parámetros de negocio (CU-30 · ALO-03). Aplican a las operaciones
 * futuras, no a las reservas ya creadas (ALO-04).
 */
@Service
public class CambiarParametrosAlojamiento {

    private final AlojamientoRepository alojamientos;

    public CambiarParametrosAlojamiento(AlojamientoRepository alojamientos) {
        this.alojamientos = alojamientos;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si un parámetro es
     *                                      inválido o los mínimos superan el catálogo actual (ALO-06)
     */
    @Transactional
    public AlojamientoResult ejecutar(CambiarParametrosAlojamientoCommand comando) {
        AlojamientoId id = new AlojamientoId(comando.alojamientoId());
        Alojamiento alojamiento = alojamientos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", id.valor()));

        alojamiento.cambiarParametros(comando.parametros().aDominio());

        alojamientos.guardar(alojamiento);
        return AlojamientoResult.de(alojamiento);
    }
}
