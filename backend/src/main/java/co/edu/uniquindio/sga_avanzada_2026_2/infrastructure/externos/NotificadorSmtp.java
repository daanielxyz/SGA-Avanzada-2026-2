package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.Enmascarar;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.Notificador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * {@link Notificador} que envía los correos por SMTP (B4b · DEC-76); se activa con {@code sga.correo.modo=smtp}.
 * El SMS todavía no tiene proveedor: no se envía y su texto no va al log, porque puede llevar un PIN.
 */
@Component
@Primary
@ConditionalOnProperty(name = "sga.correo.modo", havingValue = "smtp")
public class NotificadorSmtp implements Notificador {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorSmtp.class);

    private final JavaMailSender correo;
    private final String remitente;
    private final Executor envios;

    @Autowired
    public NotificadorSmtp(JavaMailSender correo, @Value("${sga.correo.remitente:}") String remitente,
                           @Value("${spring.mail.username:}") String usuario) {
        this(correo, remitente.isBlank() ? usuario : remitente, Executors.newVirtualThreadPerTaskExecutor());
    }

    NotificadorSmtp(JavaMailSender correo, String remitente, Executor envios) {
        if (remitente == null || remitente.isBlank()) {
            throw new IllegalStateException("sga.correo.modo=smtp necesita la cuenta de correo: defina "
                    + "SGA_CORREO_USUARIO y SGA_CORREO_CLAVE (y SGA_CORREO_REMITENTE si el remitente es otro)");
        }
        this.correo = correo;
        this.remitente = remitente;
        this.envios = envios;
    }

    /**
     * Envía el correo sin hacer esperar a quien lo pidió (DEC-68): si hay una transacción en curso, sale solo cuando
     * esta se confirma, para no avisar de algo que se deshizo; si el servidor de correo falla, queda en el log.
     */
    @Override
    public void enviarCorreo(String destino, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destino);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        alConfirmar(() -> enviar(mensaje));
    }

    // TODO(equipo): proveedor de SMS (DEC-74); mientras tanto el PIN también llega por correo (DEC-69)
    @Override
    public void enviarSms(String telefono, String texto) {
        LOG.warn("SMS sin proveedor: no se envió el mensaje para el teléfono terminado en {}", ultimos(telefono));
    }

    private void alConfirmar(Runnable envio) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envios.execute(envio);
                }
            });
        } else {
            envios.execute(envio);
        }
    }

    private void enviar(SimpleMailMessage mensaje) {
        String destino = enmascarado(mensaje.getTo()[0]);
        try {
            correo.send(mensaje);
            LOG.info("Correo enviado a {}: {}", destino, mensaje.getSubject());
        } catch (RuntimeException fallo) {
            LOG.warn("No se pudo enviar el correo a {} ({}): {}", destino, mensaje.getSubject(), fallo.getMessage());
        }
    }

    private static String enmascarado(String destino) {
        return destino != null && destino.indexOf('@') > 0 ? Enmascarar.correo(destino) : "***";
    }

    private static String ultimos(String telefono) {
        String texto = String.valueOf(telefono);
        return texto.substring(Math.max(0, texto.length() - 4));
    }
}
