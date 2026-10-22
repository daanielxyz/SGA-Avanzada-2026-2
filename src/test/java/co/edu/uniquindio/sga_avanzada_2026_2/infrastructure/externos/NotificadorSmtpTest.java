package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.Notificador;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El correo real por SMTP (DEC-76), con un servidor de correo falso que guarda lo que le llega. */
class NotificadorSmtpTest {

    private final ServidorFalso servidor = new ServidorFalso();
    private final NotificadorSmtp notificador = new NotificadorSmtp(servidor, "reservas@puertaalsol.co", Runnable::run);

    @AfterEach
    void limpiarTransaccion() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @Tag("DEC-76")
    void enviaElCorreoDesdeElRemitenteDelAlojamiento() {
        notificador.enviarCorreo("ana.rios@correo.co", "Tu reserva RES-2026-00001", "Hola Ana, tu código es 123456");

        SimpleMailMessage enviado = servidor.recibidos.getFirst();
        assertEquals("reservas@puertaalsol.co", enviado.getFrom());
        assertArrayEquals(new String[] {"ana.rios@correo.co"}, enviado.getTo());
        assertEquals("Tu reserva RES-2026-00001", enviado.getSubject());
        assertEquals("Hola Ana, tu código es 123456", enviado.getText());
    }

    @Test
    @Tag("DEC-68")
    @Tag("DEC-76")
    void conUnaTransaccionEnCursoSoloSaleSiSeConfirma() {
        TransactionSynchronizationManager.initSynchronization();
        notificador.enviarCorreo("ana.rios@correo.co", "Confirmada", "...");
        notificador.enviarCorreo("luis@correo.co", "Confirmada", "...");
        assertTrue(servidor.recibidos.isEmpty());

        List<TransactionSynchronization> pendientes = TransactionSynchronizationManager.getSynchronizations();
        pendientes.getFirst().afterCommit();

        assertEquals(1, servidor.recibidos.size());
        assertArrayEquals(new String[] {"ana.rios@correo.co"}, servidor.recibidos.getFirst().getTo());
    }

    @Test
    @Tag("DEC-68")
    void siElServidorDeCorreoFallaNoImpideLaOperacion() {
        servidor.fallar = true;

        assertDoesNotThrow(() -> notificador.enviarCorreo("ana.rios@correo.co", "Confirmada", "..."));
        assertDoesNotThrow(() -> notificador.enviarSms("3001234567", "Tu PIN es 123456"));
    }

    @Test
    @Tag("DEC-76")
    void sinCuentaDeCorreoNoArrancaEnModoSmtp() {
        assertThrows(IllegalStateException.class, () -> new NotificadorSmtp(servidor, " ", Runnable::run));
    }

    @Test
    @Tag("DEC-76")
    void elModoDeCorreoEligeElNotificador() {
        ApplicationContextRunner contexto = new ApplicationContextRunner()
                .withUserConfiguration(NotificadorLocal.class, NotificadorSmtp.class)
                .withBean(JavaMailSender.class, JavaMailSenderImpl::new);

        contexto.run(sinModo -> assertInstanceOf(NotificadorLocal.class, sinModo.getBean(Notificador.class)));
        contexto.withPropertyValues("sga.correo.modo=smtp", "spring.mail.username=reservas@puertaalsol.co")
                .run(smtp -> assertInstanceOf(NotificadorSmtp.class, smtp.getBean(Notificador.class)));
        contexto.withPropertyValues("sga.correo.modo=smtp")
                .run(sinCuenta -> assertTrue(sinCuenta.getStartupFailure() != null));
    }

    /** Guarda lo que se le envía; con {@code fallar} responde como un servidor caído. */
    private static final class ServidorFalso extends JavaMailSenderImpl {

        private final List<SimpleMailMessage> recibidos = new ArrayList<>();
        private boolean fallar;

        @Override
        public void send(SimpleMailMessage... mensajes) {
            if (fallar) {
                throw new MailSendException("Servidor de correo no disponible");
            }
            recibidos.addAll(List.of(mensajes));
        }
    }
}
