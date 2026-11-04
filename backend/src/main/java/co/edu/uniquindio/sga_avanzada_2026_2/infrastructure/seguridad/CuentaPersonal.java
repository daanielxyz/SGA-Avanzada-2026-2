package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El usuario interno y su propia cuenta: su perfil (CU-03) y su contraseña (CU-04 · USU-02). Lo identifica su token,
 * nunca un dato de la petición.
 */
@Service
public class CuentaPersonal {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder cifrador;

    public CuentaPersonal(UsuarioRepository usuarios, PasswordEncoder cifrador) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
    }

    /** @throws RecursoNoEncontradoException si el usuario del token ya no existe */
    @Transactional(readOnly = true)
    public UsuarioResult consultar(String id) {
        return UsuarioResult.de(buscar(id));
    }

    /**
     * Cambia sus datos personales; ni el correo (USU-01) ni el rol (ROL-03).
     *
     * @throws RecursoNoEncontradoException si el usuario del token ya no existe
     */
    @Transactional
    public UsuarioResult actualizar(String id, Usuario.DatosPersonales datos) {
        Usuario usuario = buscar(id);
        usuario.actualizarDatos(datos);
        usuarios.save(usuario);
        return UsuarioResult.de(usuario);
    }

    /**
     * Cambia la contraseña validando la actual (CU-04). Las demás sesiones abiertas dejan de valer, también la de esta
     * petición: hay que volver a entrar (DEC-71).
     *
     * @param nueva ya validada contra la política de contraseñas en el borde (USU-02)
     * @throws CredencialesInvalidasException si la contraseña actual no es la correcta
     */
    @Transactional
    public void cambiarContrasena(String id, String actual, String nueva) {
        Usuario usuario = buscar(id);
        if (!cifrador.matches(actual, usuario.contrasena())) {
            throw new CredencialesInvalidasException("La contraseña actual no es correcta");
        }
        usuario.fijarContrasena(cifrador.encode(nueva));
        usuarios.save(usuario);
    }

    private Usuario buscar(String id) {
        return usuarios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("el usuario", id));
    }
}
