package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Un usuario interno sin su contraseña, para devolverlo por REST (DEC-71).
 *
 * @param activado si ya fijó su contraseña con el código de activación
 */
public record UsuarioResult(String id, String nombre, String correo, String rol, String telefono,
                            String tipoDocumento, String numeroDocumento, LocalDate fechaNacimiento, boolean activado,
                            boolean eliminado) {

    public static UsuarioResult de(Usuario usuario) {
        return new UsuarioResult(usuario.id(), usuario.nombre(), usuario.correo(), usuario.rol().name(),
                usuario.telefono(), Optional.ofNullable(usuario.tipoDocumento()).map(Enum::name).orElse(null),
                usuario.numeroDocumento(), usuario.fechaNacimiento(), usuario.estaActivado(), usuario.eliminado());
    }
}
