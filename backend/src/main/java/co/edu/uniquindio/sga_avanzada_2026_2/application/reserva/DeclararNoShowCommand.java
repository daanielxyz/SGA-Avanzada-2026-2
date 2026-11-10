package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Entrada de {@link DeclararNoShowUseCase}.
 *
 * @param autor quien lo declara (BL-25)
 */
public record DeclararNoShowCommand(String codigoReserva, String autor) {
}
