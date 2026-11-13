package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.ValidacionIdentidad;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Salida de los casos de uso del Titular (DEC-31).
 *
 * @param nombre      nombres y apellidos juntos
 * @param validadoEn  cuándo recepción validó su identidad; {@code null} si no la ha validado (DEC-98)
 * @param validadoPor quién la validó
 * @param validadoPorNombre el nombre de quien la validó, para el personal (#15 · DEC-115); {@code null} si no está
 *                          validada, si ese usuario ya no existe o si la respuesta es para el huésped
 */
public record TitularResult(String id, String alojamientoId, String nombre, String tipoDocumento,
                            String numeroDocumento, String correo, String telefono, String nombres,
                            String primerApellido, String segundoApellido, boolean validado,
                            LocalDateTime validadoEn, String validadoPor, String validadoPorNombre) {

    /** Sin el nombre de quien validó: la cuenta del huésped no muestra al personal (DEC-103). */
    public static TitularResult de(Titular titular) {
        return de(titular, id -> Optional.empty());
    }

    /** Para el personal: con el nombre de quien validó la identidad (#15 · DEC-115). */
    public static TitularResult de(Titular titular, DirectorioPersonal personal) {
        Optional<ValidacionIdentidad> validacion = Optional.ofNullable(titular.validacion());
        return new TitularResult(titular.id().valor(), titular.alojamientoId().valor(), titular.nombre(),
                titular.documento().tipo().name(), titular.documento().numero(),
                Optional.ofNullable(titular.correo()).map(Correo::valor).orElse(null), titular.telefono(),
                titular.nombres(), titular.primerApellido(), titular.segundoApellido(), titular.estaValidado(),
                validacion.map(ValidacionIdentidad::fechaHora).orElse(null),
                validacion.map(v -> v.autor().valor()).orElse(null),
                validacion.flatMap(v -> personal.nombreDe(v.autor())).orElse(null));
    }
}
