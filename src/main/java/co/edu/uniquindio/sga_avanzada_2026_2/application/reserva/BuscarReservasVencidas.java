package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: el planificador pregunta qué reservas PENDIENTE ya superaron el plazo de confirmación del alojamiento
 * (CU-20 · RN-21) y luego vence cada una con {@link VencerReserva}.
 */
@Service
public class BuscarReservasVencidas {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final Clock reloj;

    public BuscarReservasVencidas(CargadorReserva cargador, ReservaRepository reservas, Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.reloj = reloj;
    }

    /**
     * Con un único alojamiento (ALO-01) todas las reservas son suyas; en un despliegue con varios habría que filtrar
     * por sus apartamentos.
     *
     * @return códigos de las reservas vencidas, de la más antigua a la más reciente
     */
    @Transactional(readOnly = true)
    public List<String> ejecutar(String alojamientoId) {
        LocalDateTime limite = LocalDateTime.now(reloj)
                .minus(cargador.alojamiento(new AlojamientoId(alojamientoId)).parametros().plazoConfirmacion());
        return reservas.buscarPendientesCreadasAntesDe(limite).stream()
                .map(r -> r.codigo().valor())
                .toList();
    }
}
