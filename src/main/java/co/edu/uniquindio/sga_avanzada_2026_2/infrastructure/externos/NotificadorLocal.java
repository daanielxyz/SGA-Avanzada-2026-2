package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.Notificador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Implementación local del {@link Notificador} (DEC-68): no envía nada; escribe cada mensaje en el log y guarda los
 * últimos en memoria, para desarrollar y probar sin servidor de correo ni de SMS. Como el PIN queda en el log, no
 * sirve para producción.
 */
// Es el que se usa salvo con sga.correo.modo=smtp, que activa NotificadorSmtp (DEC-76).
@Component
public class NotificadorLocal implements Notificador {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorLocal.class);
    private static final int GUARDADOS = 100;

    private final Deque<Mensaje> enviados = new ArrayDeque<>();

    /** Un mensaje que se habría enviado. */
    public record Mensaje(Canal canal, String destino, String asunto, String cuerpo) {
    }

    public enum Canal { CORREO, SMS }

    @Override
    public void enviarCorreo(String destino, String asunto, String cuerpo) {
        guardar(new Mensaje(Canal.CORREO, destino, asunto, cuerpo));
    }

    @Override
    public void enviarSms(String telefono, String texto) {
        guardar(new Mensaje(Canal.SMS, telefono, null, texto));
    }

    /** Los últimos mensajes, del más antiguo al más reciente. */
    public synchronized List<Mensaje> enviados() {
        return List.copyOf(enviados);
    }

    private synchronized void guardar(Mensaje mensaje) {
        LOG.info("{} local (no se envía) para {}: {}\n{}", mensaje.canal(), mensaje.destino(), mensaje.asunto(),
                mensaje.cuerpo());
        enviados.addLast(mensaje);
        if (enviados.size() > GUARDADOS) {
            enviados.removeFirst();
        }
    }
}
