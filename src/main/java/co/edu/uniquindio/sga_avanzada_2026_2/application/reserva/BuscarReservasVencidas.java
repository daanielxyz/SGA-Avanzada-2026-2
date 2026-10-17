package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: el planificador pregunta qué reservas PENDIENTE ya superaron su plazo para confirmarse (CU-20 ·
 * RN-21) y luego vence cada una con {@link VencerReserva}. La del portal tiene el plazo del pago en línea y las demás,
 * el de confirmación (DEC-69).
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
        ParametrosAlojamiento parametros = cargador.alojamiento(new AlojamientoId(alojamientoId)).parametros();
        LocalDateTime ahora = LocalDateTime.now(reloj);
        return reservas.buscarPendientesCreadasAntesDe(ahora.minus(parametros.plazoMasCorto())).stream()
                .filter(r -> r.vencio(ahora, parametros.plazoParaConfirmar(r.canalOrigen())))
                .map(r -> r.codigo().valor())
                .toList();
    }
}
