package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Inicio de sesión (CU-02): valida el correo y la contraseña y emite el token con el rol del usuario.
 */
@Service
public class IniciarSesion {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder cifrador;
    private final EmisorTokens emisor;
    /** Resumen contra el que se compara cuando el usuario no existe, para tardar lo mismo que si existiera. */
    private final String senuelo;

    public IniciarSesion(UsuarioRepository usuarios, PasswordEncoder cifrador, EmisorTokens emisor) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.emisor = emisor;
        this.senuelo = cifrador.encode("usuario-inexistente");
    }

    /**
     * CU-02 · USU-05 — un usuario eliminado lógicamente se rechaza igual que uno que no existe.
     *
     * @throws CredencialesInvalidasException si el correo no existe, el usuario fue eliminado o la contraseña no
     *                                        coincide; el mensaje es el mismo en los tres casos
     */
    @Transactional(readOnly = true)
    public SesionIniciada ejecutar(String correo, String contrasena) {
        Optional<Usuario> usuario = usuarios.findByCorreo(correo.trim().toLowerCase())
                .filter(Usuario::puedeIniciarSesion);
        boolean coincide = cifrador.matches(contrasena, usuario.map(Usuario::contrasena).orElse(senuelo));
        return usuario.filter(encontrado -> coincide)
                .map(emisor::emitir)
                .orElseThrow(CredencialesInvalidasException::new);
    }
}
