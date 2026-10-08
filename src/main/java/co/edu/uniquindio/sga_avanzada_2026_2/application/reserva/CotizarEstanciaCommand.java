package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import java.time.LocalDate;
import java.util.List;

/**
 * Entrada de {@link CotizarEstancia} (CU-09): para cotizar basta la fecha de nacimiento de cada ocupante (RN-06).
 */
public record CotizarEstanciaCommand(String apartamento, LocalDate entrada, LocalDate salida,
                                     List<LocalDate> fechasNacimiento) {
}
