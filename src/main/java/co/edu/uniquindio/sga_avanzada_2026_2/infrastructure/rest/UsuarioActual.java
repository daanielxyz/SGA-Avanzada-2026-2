package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.AccesoDenegadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * El usuario que hace la petición, tomado de su token (DEC-67): es el autor de registros, salidas, autorizaciones y
 * novedades (DEC-61). Nunca se lee del Request.
 */
@Component
public class UsuarioActual {

    /** Id del usuario, p. ej. {@code USR-7}. */
    public String id() {
        return sesion().getName();
    }

    /**
     * Para lo que, dentro de un endpoint que comparten varios roles, solo puede el administrador (DEC-22).
     *
     * @param accion lo que se intentaba, para el mensaje: «Solo el administrador puede …»
     * @throws AccesoDenegadoException si quien hace la petición no es administrador
     */
    public void exigirAdministrador(String accion) {
        boolean esAdministrador = sesion().getAuthorities().stream()
                .anyMatch(autoridad -> Rol.ADMINISTRADOR.autoridad().equals(autoridad.getAuthority()));
        if (!esAdministrador) {
            throw new AccesoDenegadoException("Solo el administrador puede " + accion);
        }
    }

    private static Authentication sesion() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
