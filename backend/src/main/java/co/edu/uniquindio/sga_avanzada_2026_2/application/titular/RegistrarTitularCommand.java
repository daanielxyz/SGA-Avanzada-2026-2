package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

/**
 * Entrada de {@link RegistrarTitularUseCase}: nombre, documento y al menos un contacto (DEC-49). Los apellidos pueden
 * llegar aparte o dentro de {@code nombres} (DEC-98).
 *
 * @param tipoDocumento nombre de {@code TipoDocumento}
 */
public record RegistrarTitularCommand(String alojamientoId, String nombres, String primerApellido,
                                      String segundoApellido, String tipoDocumento, String numeroDocumento,
                                      String correo, String telefono) {

    /** Con el nombre completo en un solo texto. */
    public RegistrarTitularCommand(String alojamientoId, String nombre, String tipoDocumento, String numeroDocumento,
                                   String correo, String telefono) {
        this(alojamientoId, nombre, null, null, tipoDocumento, numeroDocumento, correo, telefono);
    }
}
