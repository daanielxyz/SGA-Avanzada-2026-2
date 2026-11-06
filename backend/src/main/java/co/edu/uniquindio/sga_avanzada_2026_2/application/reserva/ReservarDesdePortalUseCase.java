package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.ClavesReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.PagoEnLineaDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: el huésped reserva desde el portal, sin haber iniciado sesión (CU-10 · DEC-68). Crea la reserva como
 * {@link CrearReservaUseCase} con el canal PORTAL y responde cuánto pagar en línea, hasta cuándo (DEC-69) y la clave
 * con la que solo quien reservó puede pagarla o liberarla (DEC-93).
 */
@Service
public class ReservarDesdePortalUseCase {

    private final CrearReservaUseCase crearReserva;
    private final CargadorReserva cargador;
    private final PagoEnLineaDomainService pagoEnLinea;
    private final ClavesReserva claves;
    private final Clock reloj;

    public ReservarDesdePortalUseCase(CrearReservaUseCase crearReserva, CargadorReserva cargador,
                                      PagoEnLineaDomainService pagoEnLinea, ClavesReserva claves, Clock reloj) {
        this.crearReserva = crearReserva;
        this.cargador = cargador;
        this.pagoEnLinea = pagoEnLinea;
        this.claves = claves;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException como
     *         {@link CrearReservaUseCase}
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException como
     *         {@link CrearReservaUseCase}, o si el alojamiento no tiene habilitado el pago en línea (MPAG-02), lo que
     *         se revisa antes de crear nada
     */
    @Transactional
    public ReservaPortalResult ejecutar(ReservarDesdePortalCommand comando) {
        Alojamiento delApartamento = cargador.alojamientoDe(cargador.apartamento(comando.apartamento()));
        pagoEnLinea.exigirHabilitado(delApartamento);
        // RN-09: pagar confirma la reserva y confirmar exige la hora; en el portal es la de entrada del hotel
        ReservaResult creada = crearReserva.ejecutar(new CrearReservaCommand(comando.apartamento(), comando.entrada(),
                comando.salida(), "PORTAL", comando.titular(), comando.ocupantes(),
                delApartamento.parametros().horaEntrada(), comando.carros(), comando.motos(), comando.mascotas()));
        Reserva reserva = cargador.reserva(creada.codigo());
        Alojamiento alojamiento = cargador.alojamientoDe(reserva);

        return new ReservaPortalResult(creada,
                pagoEnLinea.montoACobrar(reserva, cargador.folioDe(reserva), alojamiento, LocalDateTime.now(reloj))
                        .monto(),
                reserva.venceEn(alojamiento.parametros().plazoParaConfirmar(reserva.canalOrigen())),
                claves.emitir(reserva.codigo().valor()));
    }
}
