package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ImpuestoAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador enciende o apaga el IVA de la estancia y cambia su porcentaje (DEC-133). Aplica a
 * las llegadas que vienen; lo ya cobrado no cambia.
 */
@Service
public class CambiarImpuestoAlojamientoUseCase {

    private final AlojamientoRepository alojamientos;

    public CambiarImpuestoAlojamientoUseCase(AlojamientoRepository alojamientos) {
        this.alojamientos = alojamientos;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el porcentaje está
     *         fuera de 0 a 100, o es 0 y se pide cobrarlo
     */
    @Transactional
    public AlojamientoResult ejecutar(String alojamientoId, boolean cobraIva, int porcentaje) {
        AlojamientoId id = new AlojamientoId(alojamientoId);
        Alojamiento alojamiento = alojamientos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", id.valor()));

        alojamiento.cambiarImpuesto(new ImpuestoAlojamiento(cobraIva, new Porcentaje(porcentaje)));

        alojamientos.guardar(alojamiento);
        return AlojamientoResult.de(alojamiento);
    }
}
