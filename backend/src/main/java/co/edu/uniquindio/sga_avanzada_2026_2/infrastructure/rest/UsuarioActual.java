package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.AccesoDenegadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Quien hace la petición, tomado de su token (DEC-67): el usuario interno, autor de registros, salidas, autorizaciones
 * y novedades (DEC-61), o el titular del huésped (DEC-68). Nunca se lee del Request.
 */
@Component
public class UsuarioActual {

    /** Id del usuario, p. ej. {@code USR-7}, o del titular del huésped, p. ej. {@code TIT-7}. */
    public String id() {
        return sesion().getName();
    }

    /**
     * Un huésped solo ve y gestiona las reservas de su titular, que es su propia cuenta; el personal interno, todas
     * (USU-04 · ROL-04 · DEC-68).
     *
     * @param titularId el titular de la reserva que se pide
     * @throws AccesoDenegadoException si quien pide es un huésped y la reserva es de otro titular
     */
    public void exigirAccesoA(String titularId) {
        if (tieneRol(Rol.HUESPED) && !id().equals(titularId)) {
            throw new AccesoDenegadoException("Solo puede ver y gestionar sus propias reservas");
        }
    }

    /**
     * Para lo que, dentro de un endpoint que comparten varios roles, solo puede el administrador (DEC-22).
     *
     * @param accion lo que se intentaba, para el mensaje: «Solo el administrador puede …»
     * @throws AccesoDenegadoException si quien hace la petición no es administrador
     */
    public void exigirAdministrador(String accion) {
        if (!tieneRol(Rol.ADMINISTRADOR)) {
            throw new AccesoDenegadoException("Solo el administrador puede " + accion);
        }
    }

    /** El token de la petición, p. ej. para cerrar esa sesión (CU-48). */
    public Jwt token() {
        return ((JwtAuthenticationToken) sesion()).getToken();
    }

    private static boolean tieneRol(Rol rol) {
        return sesion().getAuthorities().stream()
                .anyMatch(autoridad -> rol.autoridad().equals(autoridad.getAuthority()));
    }

    private static Authentication sesion() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
