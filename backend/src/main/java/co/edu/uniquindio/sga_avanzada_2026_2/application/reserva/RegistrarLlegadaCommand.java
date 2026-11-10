package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Entrada de {@link RegistrarLlegadaUseCase}.
 *
 * @param autor recepcionista que registra (REG-05); es el usuario autenticado (DEC-61 · DEC-67)
 * @param motivoSinPago por qué entra sin haber pagado la estancia (BL-22); nulo si está pagada
 */
public record RegistrarLlegadaCommand(String codigo, String autor, String motivoSinPago) {
}
