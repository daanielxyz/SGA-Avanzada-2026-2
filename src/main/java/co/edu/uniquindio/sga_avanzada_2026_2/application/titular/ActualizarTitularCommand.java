package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

/**
 * Entrada de {@link ActualizarTitular}: nombre y contacto. El documento no se puede cambiar (TIT-01).
 */
public record ActualizarTitularCommand(String id, String nombre, String correo, String telefono) {
}
