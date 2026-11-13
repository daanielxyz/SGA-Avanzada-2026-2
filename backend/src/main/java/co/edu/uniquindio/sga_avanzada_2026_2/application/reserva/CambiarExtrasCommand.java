package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Entrada de {@link CambiarExtrasReservaUseCase}: los vehículos y las mascotas como quedan (DEC-38 · DEC-116).
 */
public record CambiarExtrasCommand(String codigo, int carros, int motos, int mascotas) {
}
