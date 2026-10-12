package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest;

import org.springframework.stereotype.Component;

/**
 * El usuario que hace la petición: es el autor de registros, salidas, autorizaciones y novedades (DEC-61). Nunca se
 * lee del Request.
 */
// TODO(B3): devolver el usuario del token JWT; hasta entonces todos los movimientos quedan con este autor fijo.
@Component
public class UsuarioActual {

    public static final String PROVISIONAL = "USR-PROVISIONAL";

    public String id() {
        return PROVISIONAL;
    }
}
