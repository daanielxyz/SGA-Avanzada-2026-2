package co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento;

/**
 * Entrada de {@link HabilitarMedioPago} y {@link DeshabilitarMedioPago} (CU-53).
 */
public record MedioPagoCommand(String alojamientoId, String medio) {
}
