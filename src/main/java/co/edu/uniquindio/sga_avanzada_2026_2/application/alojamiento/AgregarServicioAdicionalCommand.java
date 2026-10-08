package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

import java.math.BigDecimal;

/**
 * Entrada de {@link AgregarServicioAdicional} (CU-30 · SERV-01). El id lo genera el sistema.
 */
public record AgregarServicioAdicionalCommand(String alojamientoId, String nombre, boolean generaCargo,
                                              BigDecimal valor) {
}
