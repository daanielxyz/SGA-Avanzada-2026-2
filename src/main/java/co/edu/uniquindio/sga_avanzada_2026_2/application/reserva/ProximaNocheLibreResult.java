package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;

/**
 * Un apartamento activo y su primera noche libre para una persona.
 *
 * @param noche vacía si no hay ninguna libre en el plazo buscado
 */
public record ProximaNocheLibreResult(String codigo, LocalDate noche) {
}
