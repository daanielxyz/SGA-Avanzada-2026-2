package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import java.time.LocalDate;

/**
 * Entrada de {@link RegistrarBloqueo} (CU-32): rango [inicio, fin) y motivo (BLO-02). El id lo genera el sistema.
 */
public record RegistrarBloqueoCommand(String codigo, LocalDate inicio, LocalDate fin, String motivo) {
}
