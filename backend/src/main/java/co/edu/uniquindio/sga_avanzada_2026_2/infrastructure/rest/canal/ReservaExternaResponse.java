package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.IntegracionExternaResult;

/**
 * JSON de la respuesta al canal externo (DEC-31).
 *
 * @param resultado     EXITOSO (201), DUPLICADO (200, la reserva ya se había recibido) o CONFLICTO (409, sin
 *                      disponibilidad)
 * @param codigoReserva la reserva nueva o la que ya existía; vacío en un conflicto
 * @param conflictoId   solo en un conflicto
 * @param detalle       el código de la reserva o el motivo del conflicto
 */
public record ReservaExternaResponse(String resultado, String codigoReserva, String conflictoId, String detalle) {

    static ReservaExternaResponse de(IntegracionExternaResult r) {
        return new ReservaExternaResponse(r.resultado(), r.codigoReserva(), r.conflictoId(), r.detalle());
    }
}
