package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Ubicacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador corrige la ubicación exacta del alojamiento (CU-30 · UBI-03).
 */
@Service
public class CambiarUbicacionAlojamiento {

    private final AlojamientoRepository alojamientos;

    public CambiarUbicacionAlojamiento(AlojamientoRepository alojamientos) {
        this.alojamientos = alojamientos;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si las coordenadas
     *                                      están fuera de rango (UBI-01)
     */
    @Transactional
    public AlojamientoResult ejecutar(CambiarUbicacionAlojamientoCommand comando) {
        AlojamientoId id = new AlojamientoId(comando.alojamientoId());
        Alojamiento alojamiento = alojamientos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", id.valor()));

        alojamiento.cambiarUbicacion(new Ubicacion(comando.latitud(), comando.longitud()));

        alojamientos.guardar(alojamiento);
        return AlojamientoResult.de(alojamiento);
    }
}
