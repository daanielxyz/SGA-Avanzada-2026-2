package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import java.time.Instant;

/**
 * Resultado de {@link IniciarSesion}: el token y a quién pertenece.
 *
 * @param expiraEn instante en que el token deja de valer
 */
public record SesionIniciada(String token, Instant expiraEn, String usuario, String nombre, Rol rol) {
}
