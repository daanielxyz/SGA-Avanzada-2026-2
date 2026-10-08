package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: registrar o corregir la hora estimada de llegada, requisito para confirmar (RN-09).
 */
@Service
public class IndicarHoraLlegada {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;

    public IndicarHoraLlegada(CargadorReserva cargador, ReservaRepository reservas) {
        this.cargador = cargador;
        this.reservas = reservas;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si la
     *         reserva no existe
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva ya
     *         inició o terminó
     */
    @Transactional
    public ReservaResult ejecutar(IndicarHoraLlegadaCommand comando) {
        Reserva reserva = cargador.reserva(comando.codigo());

        reserva.indicarHoraEstimadaLlegada(comando.hora());

        reservas.guardar(reserva);
        return ReservaResult.de(reserva);
    }
}
