package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.OperacionCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ResultadoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.SentidoEvento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Caso de uso: anotar en la bitácora un mensaje con un canal externo (BIT-01). Lo usa el adaptador del canal (B4)
 * para lo que {@link RecibirReservaExterna} no anota: los mensajes RECHAZADO o ERROR y las demás operaciones del
 * contrato (BIT-03 · DEC-62). Va en su propia transacción, para que quede aunque la operación se haya deshecho.
 */
@Service
public class RegistrarEventoCanal {

    private final EventoCanalRepository eventos;
    private final CanalRepository canales;
    private final GeneradorCodigos codigos;
    private final Clock reloj;

    public RegistrarEventoCanal(EventoCanalRepository eventos, CanalRepository canales, GeneradorCodigos codigos,
                                Clock reloj) {
        this.eventos = eventos;
        this.canales = canales;
        this.codigos = codigos;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el canal
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si falta la carga útil
     *                                      (BIT-01)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EventoCanalResult ejecutar(RegistrarEventoCanalCommand comando) {
        CanalId canalId = new CanalId(comando.canalId());
        canales.buscarPorId(canalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el canal", canalId.valor()));

        EventoCanal evento = new EventoCanal(new EventoCanalId(codigos.siguiente(SerieCodigo.EVENTO)), canalId,
                OperacionCanal.valueOf(comando.operacion()), SentidoEvento.valueOf(comando.sentido()),
                LocalDateTime.now(reloj), comando.cargaUtil(), ResultadoEvento.valueOf(comando.resultado()),
                comando.detalle());

        eventos.registrar(evento);
        return EventoCanalResult.de(evento);
    }
}
