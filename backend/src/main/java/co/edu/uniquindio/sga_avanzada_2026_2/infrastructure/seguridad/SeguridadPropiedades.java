package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades {@code sga.seguridad.*}. El secreto y la clave del primer administrador llegan por variables de
 * entorno, nunca en git (DEC-67).
 */
@ConfigurationProperties("sga.seguridad")
public record SeguridadPropiedades(Jwt jwt, Administrador administrador) {

    /**
     * @param secreto         clave con la que se firman los tokens (HS256, mínimo 32 caracteres); vacío = una al azar
     *                        en cada arranque
     * @param duracionMinutos vigencia de cada token
     */
    public record Jwt(String secreto, int duracionMinutos) {
    }

    /** Primer administrador del despliegue; sin correo o sin clave no se crea. */
    public record Administrador(String nombre, String correo, String clave) {
    }
}
