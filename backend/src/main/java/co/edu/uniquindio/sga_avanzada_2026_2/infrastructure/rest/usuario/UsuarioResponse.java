package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.usuario;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.UsuarioResult;

import java.time.LocalDate;

/**
 * Un usuario interno, sin su contraseña (DEC-71).
 *
 * @param activado si ya fijó su contraseña con el código de activación
 */
public record UsuarioResponse(String id, String nombre, String correo, String rol, String telefono,
                              String tipoDocumento, String numeroDocumento, LocalDate fechaNacimiento,
                              boolean activado, boolean eliminado) {

    static UsuarioResponse de(UsuarioResult u) {
        return new UsuarioResponse(u.id(), u.nombre(), u.correo(), u.rol(), u.telefono(), u.tipoDocumento(),
                u.numeroDocumento(), u.fechaNacimiento(), u.activado(), u.eliminado());
    }
}
