package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: saber cuánto se retendría al cancelar, antes de decidirlo (DEC-91). Con la política congelada, como
 * {@link CancelarReservaUseCase}, pero sin guardar nada.
 */
@Service
public class SimularCancelacionUseCase {

    private final CargadorReserva cargador;
    private final PoliticaCancelacionRepository politicas;
    private final CancelacionDomainService cancelacion;
    private final Clock reloj;

    public SimularCancelacionUseCase(CargadorReserva cargador, PoliticaCancelacionRepository politicas,
                                     CancelacionDomainService cancelacion, Clock reloj) {
        this.cargador = cargador;
        this.politicas = politicas;
        this.cancelacion = cancelacion;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe la reserva, su folio, su política congelada, su apartamento o
     *                                      su alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva no está PENDIENTE
     *                                      ni CONFIRMADA (RN-08) o el folio está cerrado
     */
    @Transactional(readOnly = true)
    public SimulacionCancelacionResult ejecutar(String codigo) {
        Reserva reserva = cargador.reserva(codigo);
        PoliticaCancelacion politica = politicas.buscarPorId(reserva.politicaVersionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("la política",
                        reserva.politicaVersionId().valor()));
        return SimulacionCancelacionResult.de(cancelacion.simularCancelacion(reserva, cargador.folioDe(reserva),
                politica, cargador.alojamientoDe(reserva).parametros(), LocalDateTime.now(reloj)));
    }
}
