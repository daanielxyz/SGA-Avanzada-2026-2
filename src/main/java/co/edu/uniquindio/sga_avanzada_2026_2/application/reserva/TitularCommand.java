package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Datos del titular que llegan con la reserva (DEC-49): nombre, documento y al menos un contacto.
 *
 * @param tipoDocumento nombre de {@code TipoDocumento}
 */
public record TitularCommand(String nombre, String tipoDocumento, String numeroDocumento, String correo,
                             String telefono) {
}
