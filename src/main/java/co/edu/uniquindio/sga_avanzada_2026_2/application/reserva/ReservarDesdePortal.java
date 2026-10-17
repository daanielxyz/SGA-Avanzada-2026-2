package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.PagoEnLineaDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: el huésped reserva desde el portal, sin haber iniciado sesión (CU-10 · DEC-68). Crea la reserva como
 * {@link CrearReserva} con el canal PORTAL y responde cuánto pagar en línea y hasta cuándo (DEC-69).
 */
@Service
public class ReservarDesdePortal {

    private final CrearReserva crearReserva;
    private final CargadorReserva cargador;
    private final PagoEnLineaDomainService pagoEnLinea;
    private final Clock reloj;

    public ReservarDesdePortal(CrearReserva crearReserva, CargadorReserva cargador,
                               PagoEnLineaDomainService pagoEnLinea, Clock reloj) {
        this.crearReserva = crearReserva;
        this.cargador = cargador;
        this.pagoEnLinea = pagoEnLinea;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException como
     *         {@link CrearReserva}
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException como {@link CrearReserva},
     *         o si el alojamiento no tiene habilitado el pago en línea (MPAG-02), lo que se revisa antes de crear nada
     */
    @Transactional
    public ReservaPortalResult ejecutar(ReservarDesdePortalCommand comando) {
        pagoEnLinea.exigirHabilitado(cargador.alojamientoDe(cargador.apartamento(comando.apartamento())));
        ReservaResult creada = crearReserva.ejecutar(new CrearReservaCommand(comando.apartamento(), comando.entrada(),
                comando.salida(), "PORTAL", comando.titular(), comando.ocupantes(), comando.horaEstimadaLlegada()));
        Reserva reserva = cargador.reserva(creada.codigo());
        Alojamiento alojamiento = cargador.alojamientoDe(reserva);

        return new ReservaPortalResult(creada,
                pagoEnLinea.montoACobrar(reserva, cargador.folioDe(reserva), alojamiento, LocalDateTime.now(reloj))
                        .monto(),
                reserva.venceEn(alojamiento.parametros().plazoParaConfirmar(reserva.canalOrigen())));
    }
}
