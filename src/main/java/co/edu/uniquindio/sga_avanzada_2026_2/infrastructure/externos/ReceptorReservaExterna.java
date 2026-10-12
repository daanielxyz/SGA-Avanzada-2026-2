package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.IntegracionExternaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RecibirReservaExterna;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RecibirReservaExternaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarEventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarEventoCanalCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador de entrada de los canales externos: recibe la reserva que envía un canal y garantiza que el mensaje
 * quede en la bitácora también cuando se rechaza o falla (BIT-03 · DEC-62). Lo que sale bien, se repite o choca lo
 * anota el propio caso de uso.
 */
// TODO(B4): verificar aquí la credencial del canal antes de integrar el mensaje (CAN-03 · DEC-47).
@Component
public class ReceptorReservaExterna {

    private static final Logger LOG = LoggerFactory.getLogger(ReceptorReservaExterna.class);
    private static final int MAXIMO_DETALLE = 500;

    private final RecibirReservaExterna recibirReservaExterna;
    private final RegistrarEventoCanal registrarEvento;

    public ReceptorReservaExterna(RecibirReservaExterna recibirReservaExterna, RegistrarEventoCanal registrarEvento) {
        this.recibirReservaExterna = recibirReservaExterna;
        this.registrarEvento = registrarEvento;
    }

    /**
     * Integra la reserva. Si el caso de uso la rechaza o falla, la anota como RECHAZADO o ERROR y deja pasar la misma
     * excepción, para que el canal reciba su respuesta de error.
     */
    public IntegracionExternaResult recibir(RecibirReservaExternaCommand comando) {
        try {
            return recibirReservaExterna.ejecutar(comando);
        } catch (ReglaDominioException | RecursoNoEncontradoException rechazo) {
            anotar(comando, "RECHAZADO", rechazo.getMessage());
            throw rechazo;
        } catch (RuntimeException error) {
            anotar(comando, "ERROR", error.getClass().getSimpleName());
            throw error;
        }
    }

    /** Un fallo de la bitácora no debe cambiar la respuesta al canal (BIT-06): solo queda en el log. */
    private void anotar(RecibirReservaExternaCommand comando, String resultado, String detalle) {
        try {
            registrarEvento.ejecutar(new RegistrarEventoCanalCommand(comando.canalId(), "CREAR_RESERVA", "ENTRANTE",
                    comando.cargaUtil(), resultado, recortar(detalle)));
        } catch (RuntimeException fallo) {
            LOG.warn("No se pudo anotar en la bitácora el mensaje {} del canal {}: {}", comando.idExterno(),
                    comando.canalId(), fallo.getMessage());
        }
    }

    private static String recortar(String detalle) {
        String texto = String.valueOf(detalle);
        return texto.substring(0, Math.min(texto.length(), MAXIMO_DETALLE));
    }
}
