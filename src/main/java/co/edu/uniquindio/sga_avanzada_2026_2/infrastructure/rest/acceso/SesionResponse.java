package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.acceso;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.SesionIniciada;

import java.time.Instant;

/**
 * Respuesta de {@code POST /api/auth/login}: el token va en cada petición como {@code Authorization: Bearer <token>}.
 *
 * @param tipo siempre {@code Bearer}
 */
public record SesionResponse(String token, String tipo, Instant expiraEn, String usuario, String nombre, String rol) {

    static SesionResponse de(SesionIniciada sesion) {
        return new SesionResponse(sesion.token(), "Bearer", sesion.expiraEn(), sesion.usuario(), sesion.nombre(),
                sesion.rol().name());
    }
}
