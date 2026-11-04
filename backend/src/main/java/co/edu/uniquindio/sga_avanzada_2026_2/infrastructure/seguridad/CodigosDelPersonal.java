package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.Notificador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Emite y verifica los códigos del personal interno, que llegan al correo: activación de la cuenta (24 horas) y
 * recuperación de la contraseña (15 minutos) (DEC-71 · USU-06).
 */
@Component
public class CodigosDelPersonal {

    private static final Logger LOG = LoggerFactory.getLogger(CodigosDelPersonal.class);
    /** Códigos que se emiten como máximo a un usuario, por propósito, en 15 minutos. */
    private static final int MAXIMO_SOLICITUDES = 3;

    private final CodigoUsuarioRepository codigos;
    private final PasswordEncoder cifrador;
    private final Notificador notificador;
    private final Clock reloj;

    public CodigosDelPersonal(CodigoUsuarioRepository codigos, PasswordEncoder cifrador, Notificador notificador,
                              Clock reloj) {
        this.codigos = codigos;
        this.cifrador = cifrador;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    /**
     * Envía un código nuevo al correo del usuario; el nuevo deja sin efecto los anteriores. Pasado el máximo de
     * solicitudes no envía nada.
     */
    public void enviar(Usuario usuario, CodigoUsuario.Proposito proposito) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (codigos.countByUsuarioIdAndPropositoAndCreadoEnAfter(usuario.id(), proposito, ahora.minusMinutes(15))
                >= MAXIMO_SOLICITUDES) {
            LOG.warn("Se pidieron demasiados códigos de {} para {}: no se envía otro", proposito, usuario.id());
            return;
        }
        String codigo = CodigoDeUnSoloUso.nuevoPin();
        codigos.save(CodigoUsuario.emitir(usuario.id(), proposito, cifrador.encode(codigo), ahora));
        notificador.enviarCorreo(usuario.correo(), asunto(proposito), texto(usuario, proposito, codigo));
    }

    /**
     * @return si el código del usuario para ese propósito vale; deja constancia del intento en cualquier caso
     */
    public boolean verificar(Usuario usuario, CodigoUsuario.Proposito proposito, String codigo) {
        Optional<CodigoUsuario> vigente = codigos.findFirstByUsuarioIdAndPropositoOrderByIdDesc(usuario.id(),
                proposito);
        boolean vale = vigente.map(c -> c.verificar(codigo, LocalDateTime.now(reloj), cifrador)).orElse(false);
        vigente.ifPresent(codigos::save);
        return vale;
    }

    private static String asunto(CodigoUsuario.Proposito proposito) {
        return proposito == CodigoUsuario.Proposito.ACTIVACION ? "Activa tu cuenta" : "Recupera tu contraseña";
    }

    private static String texto(Usuario usuario, CodigoUsuario.Proposito proposito, String codigo) {
        return proposito == CodigoUsuario.Proposito.ACTIVACION
                ? "Hola " + usuario.nombre() + ":\n\nTe crearon una cuenta como " + usuario.rol() + ". Tu código"
                        + " para fijar la contraseña es " + codigo + ". Vence en 24 horas y sirve una sola vez.\n"
                : "Hola " + usuario.nombre() + ":\n\nTu código para fijar una contraseña nueva es " + codigo
                        + ". Vence en 15 minutos y sirve una sola vez. Si no lo pediste, ignora este mensaje.\n";
    }
}
