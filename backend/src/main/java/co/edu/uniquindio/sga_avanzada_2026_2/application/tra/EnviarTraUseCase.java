package co.edu.uniquindio.sga_avanzada_2026_2.application.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CargadorReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EnvioTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.EnvioTraRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.HuespedTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.ReporteTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.ReporteTraDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.TraNoDisponibleException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.TransmisorTra;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: envía al Ministerio el TRA de una reserva (DEC-130). Lo usan el planificador, con los pendientes, y
 * recepción con «Reintentar». No tiene una transacción larga: cada paso que entra al Ministerio se guarda enseguida,
 * para no repetirlo si el siguiente falla. Que el Ministerio no lo reciba (o que falte el RNT) no es un error de la
 * petición: el envío queda CON_ERROR con el motivo.
 */
@Service
public class EnviarTraUseCase {

    private final CargadorReserva cargador;
    private final EnvioTraRepository envios;
    private final ReporteTraDomainService reportes;
    private final TransmisorTra transmisor;
    private final String tipoAcomodacion;
    private final Clock reloj;

    public EnviarTraUseCase(CargadorReserva cargador, EnvioTraRepository envios, ReporteTraDomainService reportes,
                            TransmisorTra transmisor,
                            @Value("${sga.tra.tipo-acomodacion:Apartamento}") String tipoAcomodacion, Clock reloj) {
        this.cargador = cargador;
        this.envios = envios;
        this.reportes = reportes;
        this.transmisor = transmisor;
        this.tipoAcomodacion = tipoAcomodacion;
        this.reloj = reloj;
    }

    /**
     * Una reserva que llegó antes de DEC-130 no tiene envío: se crea aquí.
     *
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si no existe
     *         la reserva, su apartamento o su alojamiento
     * @throws ReglaDominioException si ya se envió, el grupo no ha llegado o a alguien le falta un dato del TRA: el
     *         planificador lo deja para la siguiente vuelta y a recepción se le dice por qué (409)
     */
    public EnvioTraResult ejecutar(String codigoReserva) {
        Reserva reserva = cargador.reserva(codigoReserva);
        EnvioTra envio = envios.buscarPorReserva(reserva.codigo()).orElseGet(() -> EnvioTra.pendiente(reserva.codigo()));
        envio.exigirSinEnviar();
        reportes.exigirListaParaEnviar(reserva);

        Alojamiento alojamiento = cargador.alojamientoDe(reserva);
        envio.iniciarIntento(LocalDateTime.now(reloj));
        try {
            ReporteTra reporte = reportes.armar(reserva, alojamiento.nombre(), alojamiento.rnt(), tipoAcomodacion);
            transmitir(envio, reporte);
            envio.terminar(LocalDateTime.now(reloj));
        } catch (TraNoDisponibleException | ReglaDominioException fallo) {
            envio.fallar(fallo.getMessage());
        }
        envios.guardar(envio);
        return EnvioTraResult.de(envio);
    }

    /** Primero el huésped principal, después cada acompañante con su código; lo que ya entró no se repite. */
    private void transmitir(EnvioTra envio, ReporteTra reporte) {
        if (!envio.principalEnviado()) {
            envio.registrarPrincipal(transmisor.enviarPrincipal(reporte));
            envios.guardar(envio);
        }
        for (HuespedTra acompanante : envio.acompanantesPorEnviar(reporte)) {
            transmisor.enviarAcompanante(reporte, acompanante, envio.codigoPrincipal());
            envio.registrarAcompanante(acompanante.ocupante());
            envios.guardar(envio);
        }
    }
}
