package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.TitularResult;

import java.time.LocalDateTime;

/**
 * JSON de un titular (DEC-31): el responsable de una o varias reservas (TIT-05).
 *
 * @param nombre      nombres y apellidos juntos
 * @param validado    si recepción validó su identidad en persona; entonces su nombre solo lo cambia el hotel (DEC-98)
 * @param validadoEn  cuándo; {@code null} si no está validado
 * @param validadoPor el usuario que la validó
 */
public record TitularResponse(String id, String nombre, String tipoDocumento, String numeroDocumento, String correo,
                              String telefono, String nombres, String primerApellido, String segundoApellido,
                              boolean validado, LocalDateTime validadoEn, String validadoPor) {

    static TitularResponse de(TitularResult r) {
        return new TitularResponse(r.id(), r.nombre(), r.tipoDocumento(), r.numeroDocumento(), r.correo(),
                r.telefono(), r.nombres(), r.primerApellido(), r.segundoApellido(), r.validado(), r.validadoEn(),
                r.validadoPor());
    }
}
