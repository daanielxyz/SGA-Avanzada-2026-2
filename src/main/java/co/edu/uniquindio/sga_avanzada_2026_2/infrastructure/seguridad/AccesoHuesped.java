package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.Enmascarar;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.Notificador;
import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.CambiarCorreoTitular;
import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.CambiarCorreoTitularCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * La cuenta del huésped, que es su titular (DEC-68): inicio de sesión con correo, número de documento y un PIN que
 * llega al correo, y cambio de correo con un PIN que llega al teléfono y al correo actual (DEC-69). Las solicitudes
 * de PIN responden igual exista o no la cuenta, para no revelar quién tiene una.
 */
@Service
public class AccesoHuesped {

    private static final Logger LOG = LoggerFactory.getLogger(AccesoHuesped.class);
    private static final Duration VIGENCIA = Duration.ofMinutes(15);
    /** PIN que se emiten como máximo a un titular, por propósito, en una ventana de vigencia. */
    private static final int MAXIMO_SOLICITUDES = 3;
    private static final String NO_VALE = "Los datos o el PIN no son válidos, o el PIN ya venció";

    private final TitularRepository titulares;
    private final CodigoAccesoRepository codigos;
    private final CambiarCorreoTitular cambiarCorreo;
    private final EmisorTokens emisor;
    private final PasswordEncoder cifrador;
    private final Notificador notificador;
    private final Clock reloj;
    private final AlojamientoId alojamientoId;

    public AccesoHuesped(TitularRepository titulares, CodigoAccesoRepository codigos,
                         CambiarCorreoTitular cambiarCorreo, EmisorTokens emisor, PasswordEncoder cifrador,
                         Notificador notificador, Clock reloj,
                         @Value("${sga.alojamiento.id:ALO-1}") String alojamientoId) {
        this.titulares = titulares;
        this.codigos = codigos;
        this.cambiarCorreo = cambiarCorreo;
        this.emisor = emisor;
        this.cifrador = cifrador;
        this.notificador = notificador;
        this.reloj = reloj;
        this.alojamientoId = new AlojamientoId(alojamientoId);
    }

    /**
     * Primer paso para entrar: si el correo y el documento son de una cuenta, le envía un PIN a ese correo (DEC-68).
     * Si no, no hace nada, y quien pide no lo nota.
     */
    @Transactional
    public void solicitarPin(String correo, String numeroDocumento) {
        titularDe(correo, numeroDocumento).ifPresent(titular ->
                emitirPin(titular, PropositoCodigo.INICIO_SESION, null).ifPresent(pin ->
                        notificador.enviarCorreo(titular.correo().valor(), "Tu PIN para entrar",
                                "Tu PIN para entrar a tu cuenta es " + pin + ". Vence en 15 minutos y sirve una sola"
                                        + " vez. Si no fuiste tú, ignora este mensaje.")));
    }

    /**
     * Segundo paso para entrar (DEC-68): con el PIN correcto emite el token del huésped, con su titular como sujeto.
     *
     * @throws CredencialesInvalidasException si los datos no son de una cuenta o el PIN no vale (equivocado, usado,
     *                                        vencido o después de 5 intentos); el mensaje es el mismo
     */
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public SesionIniciada iniciarSesion(String correo, String numeroDocumento, String pin) {
        Titular titular = titularDe(correo, numeroDocumento)
                .orElseThrow(() -> new CredencialesInvalidasException(NO_VALE));
        verificar(titular, PropositoCodigo.INICIO_SESION, pin);
        return emisor.emitir(titular.id().valor(), titular.nombre(), Rol.HUESPED);
    }

    /**
     * Primer paso del cambio de correo, sin sesión (DEC-69): envía un PIN al teléfono y al correo actual del
     * titular, así lo puede cambiar aunque haya perdido uno de los dos.
     *
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el correo nuevo es
     *                                      inválido
     */
    @Transactional
    public void solicitarCambioCorreo(String tipoDocumento, String numeroDocumento, String nuevoCorreo) {
        Correo nuevo = new Correo(nuevoCorreo);
        String texto = "Tu PIN para cambiar el correo de tu cuenta a %s es %s. Vence en 15 minutos. Si no fuiste tú,"
                + " ignora este mensaje y tu correo no cambiará.";
        titularPorDocumento(tipoDocumento, numeroDocumento).ifPresent(titular ->
                emitirPin(titular, PropositoCodigo.CAMBIO_CORREO, nuevo.valor()).ifPresent(pin -> {
                    String mensaje = texto.formatted(Enmascarar.correo(nuevo.valor()), pin);
                    Optional.ofNullable(titular.telefono()).ifPresent(telefono ->
                            notificador.enviarSms(telefono, mensaje));
                    Optional.ofNullable(titular.correo()).ifPresent(actual ->
                            notificador.enviarCorreo(actual.valor(), "Cambio del correo de tu cuenta", mensaje));
                }));
    }

    /**
     * Segundo paso del cambio de correo (DEC-69): con el PIN correcto cambia el correo de la cuenta y lo avisa al
     * correo anterior y al nuevo.
     *
     * @throws CredencialesInvalidasException si el documento no es de un titular o el PIN no vale
     */
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public void confirmarCambioCorreo(String tipoDocumento, String numeroDocumento, String pin) {
        Titular titular = titularPorDocumento(tipoDocumento, numeroDocumento)
                .orElseThrow(() -> new CredencialesInvalidasException(NO_VALE));
        String nuevo = verificar(titular, PropositoCodigo.CAMBIO_CORREO, pin).nuevoCorreo();
        Optional<Correo> anterior = Optional.ofNullable(titular.correo());

        cambiarCorreo.ejecutar(new CambiarCorreoTitularCommand(titular.id().valor(), nuevo));

        anterior.ifPresent(correo -> notificador.enviarCorreo(correo.valor(), "El correo de tu cuenta cambió",
                "El correo de tu cuenta ahora es " + Enmascarar.correo(nuevo) + ". Si no fuiste tú, comunícate con el"
                        + " alojamiento."));
        notificador.enviarCorreo(nuevo, "Este es ahora el correo de tu cuenta",
                "Desde ahora entras a tu cuenta con este correo y tu número de documento.");
    }

    private Optional<Titular> titularDe(String correo, String numeroDocumento) {
        return titulares.buscarPorCorreo(alojamientoId, new Correo(correo)).stream()
                .filter(titular -> titular.documento().numero().equals(numeroDocumento.trim()))
                .findFirst();
    }

    private Optional<Titular> titularPorDocumento(String tipoDocumento, String numeroDocumento) {
        return titulares.buscarPorDocumento(alojamientoId,
                new Documento(TipoDocumento.valueOf(tipoDocumento), numeroDocumento));
    }

    /** Un PIN nuevo deja sin efecto los anteriores; pasado el máximo de solicitudes no se emite otro (DEC-68). */
    private Optional<String> emitirPin(Titular titular, PropositoCodigo proposito, String nuevoCorreo) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        String titularId = titular.id().valor();
        if (codigos.countByTitularIdAndPropositoAndCreadoEnAfter(titularId, proposito, ahora.minus(VIGENCIA))
                >= MAXIMO_SOLICITUDES) {
            LOG.warn("Se pidieron demasiados PIN de {} para {}: no se envía otro", proposito, titularId);
            return Optional.empty();
        }
        String pin = CodigoDeUnSoloUso.nuevoPin();
        codigos.save(CodigoAcceso.emitir(titularId, proposito, cifrador.encode(pin), nuevoCorreo, ahora,
                ahora.plus(VIGENCIA)));
        return Optional.of(pin);
    }

    private CodigoAcceso verificar(Titular titular, PropositoCodigo proposito, String pin) {
        CodigoAcceso codigo = codigos.findFirstByTitularIdAndPropositoOrderByIdDesc(titular.id().valor(), proposito)
                .orElseThrow(() -> new CredencialesInvalidasException(NO_VALE));
        boolean vale = codigo.verificar(pin, LocalDateTime.now(reloj), cifrador);
        codigos.save(codigo);
        if (!vale) {
            throw new CredencialesInvalidasException(NO_VALE);
        }
        return codigo;
    }
}
