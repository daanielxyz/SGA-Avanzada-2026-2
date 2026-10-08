package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: el administrador habilita un medio de pago del alojamiento (CU-53 · MPAG-06).
 */
@Service
public class HabilitarMedioPago {

    private final AlojamientoRepository alojamientos;

    public HabilitarMedioPago(AlojamientoRepository alojamientos) {
        this.alojamientos = alojamientos;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si ya estaba habilitado
     */
    @Transactional
    public AlojamientoResult ejecutar(MedioPagoCommand comando) {
        AlojamientoId id = new AlojamientoId(comando.alojamientoId());
        Alojamiento alojamiento = alojamientos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", id.valor()));

        alojamiento.habilitarMedioPago(new MedioPago(comando.medio()));

        alojamientos.guardar(alojamiento);
        return AlojamientoResult.de(alojamiento);
    }
}
