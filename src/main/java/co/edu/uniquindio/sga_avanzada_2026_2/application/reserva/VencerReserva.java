package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: el sistema cancela una reserva PENDIENTE que superó el plazo de confirmación (CU-20 · RN-21). El
 * planificador (B4) lo llama una vez por cada código de {@link BuscarReservasVencidas}, cada una en su propia
 * transacción, para que un fallo no detenga las demás (DEC-57).
 */
@Service
public class VencerReserva {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final CancelacionDomainService cancelacion;
    private final Clock reloj;

    public VencerReserva(CargadorReserva cargador, ReservaRepository reservas, FolioRepository folios,
                         CancelacionDomainService cancelacion, Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.folios = folios;
        this.cancelacion = cancelacion;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no
     *         existe la reserva, su folio, su apartamento o su alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si ya no está PENDIENTE
     *         (p. ej. la confirmaron mientras tanto) o todavía no supera el plazo
     */
    @Transactional
    public CancelacionResult ejecutar(String codigo) {
        Reserva reserva = cargador.reserva(codigo);
        Folio folio = cargador.folioDe(reserva);
        Alojamiento alojamiento = cargador.alojamientoDe(cargador.apartamento(reserva.apartamentoId()));

        cancelacion.procesarVencimiento(reserva, folio, alojamiento.parametros(), LocalDateTime.now(reloj));

        reservas.guardar(reserva);
        folios.guardar(folio);
        return CancelacionResult.de(reserva, Dinero.CERO, folio);
    }
}
