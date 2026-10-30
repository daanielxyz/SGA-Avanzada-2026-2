package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal.ApartamentoPublicoResponse.PrecioResponse;

import java.time.LocalDate;

/**
 * JSON de unas fechas y un grupo en un apartamento, para la página del apartamento en el portal. No dice por qué no
 * está libre: el motivo del dominio puede nombrar reservas de otros huéspedes.
 *
 * @param disponible        si se puede reservar con esas fechas y ese grupo (RN-01 · RN-02 · RN-07 · RN-20)
 * @param precio            lo que costaría esa estancia para el grupo, esté libre o no (RN-05 · RN-06)
 * @param proximaNocheLibre la primera noche libre desde la llegada pedida; si está libre, es la misma llegada
 */
public record EstanciaPortalResponse(boolean disponible, PrecioResponse precio, LocalDate proximaNocheLibre) {
}
