package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.NoShowDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: saber cuánto se retendría al declarar el no-show, antes de hacerlo (DEC-102). Con la política
 * congelada, como {@link DeclararNoShowUseCase}, pero sin guardar nada.
 */
@Service
public class SimularNoShowUseCase {

    private final CargadorReserva cargador;
    private final PoliticaCancelacionRepository politicas;
    private final NoShowDomainService noShow;
    private final Clock reloj;

    public SimularNoShowUseCase(CargadorReserva cargador, PoliticaCancelacionRepository politicas,
                                NoShowDomainService noShow, Clock reloj) {
        this.cargador = cargador;
        this.politicas = politicas;
        this.noShow = noShow;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe la reserva, su folio, su política congelada, su apartamento o
     *                                      su alojamiento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la reserva no está
     *                                      CONFIRMADA (RN-08), aún no llega la hora límite (RES-16) o el folio está
     *                                      cerrado (CAR-07)
     */
    @Transactional(readOnly = true)
    public SimulacionCancelacionResult ejecutar(String codigo) {
        Reserva reserva = cargador.reserva(codigo);
        PoliticaCancelacion politica = politicas.buscarPorId(reserva.politicaVersionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("la política",
                        reserva.politicaVersionId().valor()));
        return SimulacionCancelacionResult.de(noShow.simularNoShow(reserva, cargador.folioDe(reserva), politica,
                cargador.alojamientoDe(reserva).parametros(), LocalDateTime.now(reloj)));
    }
}
