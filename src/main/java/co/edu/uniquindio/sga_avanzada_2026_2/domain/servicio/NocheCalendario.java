package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import java.time.LocalDate;

/**
 * Calculado (no se guarda): si una noche de un apartamento se puede vender (CU-08).
 */
public record NocheCalendario(LocalDate fecha, boolean libre) {
}
