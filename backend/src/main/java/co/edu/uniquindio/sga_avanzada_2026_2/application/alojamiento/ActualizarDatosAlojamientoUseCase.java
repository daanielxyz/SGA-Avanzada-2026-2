package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador corrige los datos del alojamiento y sus normas (CU-30 · ALO-02).
 */
@Service
public class ActualizarDatosAlojamientoUseCase {

    private final AlojamientoRepository alojamientos;

    public ActualizarDatosAlojamientoUseCase(AlojamientoRepository alojamientos) {
        this.alojamientos = alojamientos;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si falta un dato obligatorio (ALO-02)
     */
    @Transactional
    public AlojamientoResult ejecutar(ActualizarDatosAlojamientoCommand comando) {
        AlojamientoId id = new AlojamientoId(comando.alojamientoId());
        Alojamiento alojamiento = alojamientos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", id.valor()));

        alojamiento.actualizarDatos(comando.nombre(), comando.descripcion(), comando.ciudad(), comando.direccion(),
                comando.normas());

        alojamientos.guardar(alojamiento);
        return AlojamientoResult.de(alojamiento);
    }
}
