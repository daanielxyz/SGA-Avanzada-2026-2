package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.Enmascarar;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.Notificador;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * Los avisos al titular cuando se crea una reserva por cualquier canal (DEC-68): un correo con la reserva y otro con
 * su cuenta, los dos al correo de la cuenta. Un titular sin correo no recibe nada.
 */
@Component
public class AvisosReserva {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    private final Notificador notificador;

    public AvisosReserva(Notificador notificador) {
        this.notificador = notificador;
    }

    /**
     * DEC-68 — el segundo correo anuncia la cuenta nueva o, si ya la tenía, le recuerda que puede gestionar sus
     * reservas desde ella.
     *
     * @param alojamiento nombre del alojamiento, para los textos
     */
    public void avisarCreacion(Reserva reserva, TitularParaReserva paraReserva, String alojamiento) {
        Titular titular = paraReserva.titular();
        Optional.ofNullable(titular.correo()).map(Correo::valor).ifPresent(destino -> {
            notificador.enviarCorreo(destino, "Tu reserva " + reserva.codigo().valor() + " en " + alojamiento,
                    detalle(reserva, titular, alojamiento));
            notificador.enviarCorreo(destino, paraReserva.cuentaNueva()
                            ? "Ya tienes una cuenta en " + alojamiento
                            : "Gestiona tus reservas en " + alojamiento,
                    cuenta(titular, alojamiento, paraReserva.cuentaNueva()));
        });
    }

    /**
     * Lo que se le dice a quien reservó con un correo distinto al de la cuenta del titular, con ese correo enmascarado
     * porque quien reserva todavía no probó ser su dueño (DEC-69).
     *
     * @return el aviso, o nulo si no hace falta
     */
    public String avisoCuenta(TitularParaReserva paraReserva) {
        return Optional.of(paraReserva)
                .filter(TitularParaReserva::correoDistinto)
                .map(p -> "El titular ya tiene una cuenta con el correo " + Enmascarar.correo(p.titular().correo()
                        .valor()) + ": los avisos de esta reserva se enviaron ahí. Para cambiarlo, solicite el PIN de"
                        + " cambio de correo, que llega al teléfono y al correo de la cuenta.")
                .orElse(null);
    }

    private static String detalle(Reserva reserva, Titular titular, String alojamiento) {
        return "Hola " + titular.nombre() + ":\n\n"
                + "Recibimos tu reserva en " + alojamiento + ".\n\n"
                + "Código: " + reserva.codigo().valor() + "\n"
                + "Apartamento: " + reserva.apartamentoId().valor() + "\n"
                + "Entrada: " + reserva.estancia().entrada() + "\n"
                + "Salida: " + reserva.estancia().salida() + "\n"
                + "Personas: " + reserva.ocupantes().size() + "\n"
                + "Valor total: $" + String.format(COLOMBIA, "%,d", reserva.valorTotal().monto().longValue()) + "\n"
                + "Estado: " + reserva.estado() + "\n";
    }

    private static String cuenta(Titular titular, String alojamiento, boolean nueva) {
        String acceso = "Entra con este correo y tu número de documento; cada vez te enviaremos un PIN a este correo"
                + " para confirmar que eres tú.";
        return nueva
                ? "Hola " + titular.nombre() + ":\n\nCon tu reserva quedó creada tu cuenta en " + alojamiento
                        + ". Desde ella puedes ver y gestionar tus reservas.\n\n" + acceso + "\n"
                : "Hola " + titular.nombre() + ":\n\nRecuerda que puedes ver y gestionar esta y tus demás reservas"
                        + " desde tu cuenta en " + alojamiento + ".\n\n" + acceso + "\n";
    }
}
