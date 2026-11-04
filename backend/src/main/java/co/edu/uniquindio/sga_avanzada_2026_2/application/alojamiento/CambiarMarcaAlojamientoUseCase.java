package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador cambia la marca que muestra la web de reservas, sin desplegar código (DEC-83 · BL-05).
 */
@Service
public class CambiarMarcaAlojamientoUseCase {

    private final AlojamientoRepository alojamientos;

    public CambiarMarcaAlojamientoUseCase(AlojamientoRepository alojamientos) {
        this.alojamientos = alojamientos;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si un valor no tiene el
     *         formato de la marca
     */
    @Transactional
    public AlojamientoResult ejecutar(CambiarMarcaAlojamientoCommand comando) {
        AlojamientoId id = new AlojamientoId(comando.alojamientoId());
        Alojamiento alojamiento = alojamientos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", id.valor()));

        alojamiento.cambiarMarca(comando.marca().aDominio());

        alojamientos.guardar(alojamiento);
        return AlojamientoResult.de(alojamiento);
    }
}
