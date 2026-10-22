package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

/**
 * Entrada de {@link ActualizarCuentaTitularUseCase}: lo que el huésped cambia desde su cuenta.
 */
public record ActualizarCuentaTitularCommand(String id, String nombre, String telefono) {
}
