package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.TitularResult;

/**
 * JSON de un titular (DEC-31): el responsable de una o varias reservas (TIT-05).
 */
public record TitularResponse(String id, String nombre, String tipoDocumento, String numeroDocumento, String correo,
                              String telefono) {

    static TitularResponse de(TitularResult r) {
        return new TitularResponse(r.id(), r.nombre(), r.tipoDocumento(), r.numeroDocumento(), r.correo(),
                r.telefono());
    }
}
