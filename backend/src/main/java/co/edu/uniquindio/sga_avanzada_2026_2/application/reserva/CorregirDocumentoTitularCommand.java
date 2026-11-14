package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Entrada de {@link CorregirDocumentoTitularUseCase} (#14 · DEC-120).
 *
 * @param tipoDocumento nombre de {@code TipoDocumento}
 * @param autor         id del usuario de la sesión, nunca del cuerpo (DEC-61)
 */
public record CorregirDocumentoTitularCommand(String codigo, String tipoDocumento, String numeroDocumento,
                                              String motivo, String autor) {
}
