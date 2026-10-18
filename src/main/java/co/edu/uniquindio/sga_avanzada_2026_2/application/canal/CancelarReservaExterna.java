package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CancelacionResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CancelarReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.OperacionCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ResultadoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.SentidoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: un canal externo cancela una reserva que él mismo trajo (CU-16 · CAN-03 · DEC-73). Se cancela como en
 * recepción, con la política congelada (RN-12 · RN-13), y queda en la bitácora del canal (BIT-01). Un canal
 * desactivado todavía puede cancelar lo suyo (CAN-05).
 */
@Service
public class CancelarReservaExterna {

    private final CanalRepository canales;
    private final ReservaRepository reservas;
    private final EventoCanalRepository eventos;
    private final CancelarReserva cancelarReserva;
    private final GeneradorCodigos codigos;
    private final Clock reloj;

    public CancelarReservaExterna(CanalRepository canales, ReservaRepository reservas, EventoCanalRepository eventos,
                                  CancelarReserva cancelarReserva, GeneradorCodigos codigos, Clock reloj) {
        this.canales = canales;
        this.reservas = reservas;
        this.eventos = eventos;
        this.cancelarReserva = cancelarReserva;
        this.codigos = codigos;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el canal o ese canal no trajo una reserva con ese
     *                                      identificador (RN-19)
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el canal no es
     *         externo o la reserva ya no se puede cancelar (RN-08)
     */
    @Transactional
    public CancelacionResult ejecutar(CancelarReservaExternaCommand comando) {
        CanalId canalId = new CanalId(comando.canalId());
        Canal canal = canales.buscarPorId(canalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el canal", canalId.valor()));
        canal.exigirExterno();
        Reserva reserva = reservas.buscarPorCanalEIdExterno(canal.id(), comando.idExterno())
                .orElseThrow(() -> new RecursoNoEncontradoException("la reserva del canal con el identificador",
                        comando.idExterno()));

        CancelacionResult cancelada = cancelarReserva.ejecutar(reserva.codigo().valor());

        eventos.registrar(new EventoCanal(new EventoCanalId(codigos.siguiente(SerieCodigo.EVENTO)), canal.id(),
                OperacionCanal.CANCELAR_RESERVA, SentidoEvento.ENTRANTE, LocalDateTime.now(reloj),
                comando.cargaUtil(), ResultadoEvento.EXITOSO, "Reserva " + reserva.codigo().valor() + " cancelada"));
        return cancelada;
    }
}
