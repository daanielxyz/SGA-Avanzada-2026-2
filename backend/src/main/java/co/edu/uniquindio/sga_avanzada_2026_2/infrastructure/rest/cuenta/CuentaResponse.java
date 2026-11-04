package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.cuenta;

import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.TitularResult;

/**
 * La cuenta del huésped, que son los datos de su titular (DEC-68).
 *
 * @param id el del titular
 */
public record CuentaResponse(String id, String nombre, String tipoDocumento, String numeroDocumento, String correo,
                             String telefono) {

    static CuentaResponse de(TitularResult titular) {
        return new CuentaResponse(titular.id(), titular.nombre(), titular.tipoDocumento(), titular.numeroDocumento(),
                titular.correo(), titular.telefono());
    }
}
