package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

/**
 * Entrada de {@link RegistrarTitular}: nombre, documento y al menos un contacto (DEC-49).
 *
 * @param tipoDocumento nombre de {@code TipoDocumento}
 */
public record RegistrarTitularCommand(String alojamientoId, String nombre, String tipoDocumento,
                                      String numeroDocumento, String correo, String telefono) {
}
