package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.NocheCalendario;

import java.time.LocalDate;

/**
 * JSON de una noche del calendario de un apartamento (CU-08).
 */
public record NocheCalendarioResponse(LocalDate fecha, boolean libre) {

    static NocheCalendarioResponse de(NocheCalendario noche) {
        return new NocheCalendarioResponse(noche.fecha(), noche.libre());
    }
}
