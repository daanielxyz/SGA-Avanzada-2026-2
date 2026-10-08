package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador retira un servicio del catálogo sin borrarlo (SERV-04).
 */
@Service
public class DesactivarServicio {

    private final AlojamientoRepository alojamientos;

    public DesactivarServicio(AlojamientoRepository alojamientos) {
        this.alojamientos = alojamientos;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el servicio no
     *                                      existe, ya estaba inactivo o se quedaría por debajo del mínimo (SERV-05)
     */
    @Transactional
    public AlojamientoResult ejecutar(DesactivarServicioCommand comando) {
        AlojamientoId id = new AlojamientoId(comando.alojamientoId());
        Alojamiento alojamiento = alojamientos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", id.valor()));

        alojamiento.desactivarServicio(new ServicioAdicionalId(comando.servicioId()));

        alojamientos.guardar(alojamiento);
        return AlojamientoResult.de(alojamiento);
    }
}
