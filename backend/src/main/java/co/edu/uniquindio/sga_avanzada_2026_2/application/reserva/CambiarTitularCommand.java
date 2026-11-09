package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Entrada de {@link CambiarTitularUseCase}: quién pasa a ser el titular. Si ya existe un titular con ese documento,
 * se usa ese y solo se completa su contacto (TIT-05 · DEC-69).
 */
public record CambiarTitularCommand(String codigo, TitularCommand titular) {
}
