package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * El personal interno fija su contraseña con un código que le llega al correo: la primera vez, con el de activación,
 * y cuando la olvida, con el de recuperación (CU-05 · USU-06 · USU-08 · DEC-71). Sin sesión.
 */
@Service
public class RecuperacionContrasena {

    private static final String NO_VALE = "El correo o el código no son válidos, o el código ya venció";

    private final UsuarioRepository usuarios;
    private final CodigosDelPersonal codigos;
    private final PasswordEncoder cifrador;

    public RecuperacionContrasena(UsuarioRepository usuarios, CodigosDelPersonal codigos, PasswordEncoder cifrador) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.cifrador = cifrador;
    }

    /**
     * Envía el código de recuperación si el correo es de un usuario que no fue eliminado. Si no, no hace nada, y quien
     * pide no lo nota (USU-08).
     */
    @Transactional
    public void solicitar(String correo) {
        usuario(correo).ifPresent(usuario -> codigos.enviar(usuario, CodigoUsuario.Proposito.RECUPERACION));
    }

    /**
     * Fija la contraseña con el código; las sesiones abiertas antes dejan de valer (DEC-71).
     *
     * @param nueva ya validada contra la política de contraseñas en el borde (USU-02)
     * @throws CredencialesInvalidasException si el correo no es de un usuario activo o el código no vale; el mensaje
     *                                        es el mismo
     */
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public void fijar(String correo, CodigoUsuario.Proposito proposito, String codigo, String nueva) {
        Usuario usuario = usuario(correo)
                .filter(encontrado -> codigos.verificar(encontrado, proposito, codigo))
                .orElseThrow(() -> new CredencialesInvalidasException(NO_VALE));
        usuario.fijarContrasena(cifrador.encode(nueva));
        usuarios.save(usuario);
    }

    private Optional<Usuario> usuario(String correo) {
        return usuarios.findByCorreo(correo.trim().toLowerCase()).filter(usuario -> !usuario.eliminado());
    }
}
