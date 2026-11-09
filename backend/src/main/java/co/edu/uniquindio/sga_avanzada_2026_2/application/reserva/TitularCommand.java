package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Datos del titular que llegan con la reserva (DEC-49): nombre, documento y al menos un contacto. Los apellidos
 * pueden llegar aparte o dentro de {@code nombres} (DEC-98).
 *
 * @param tipoDocumento nombre de {@code TipoDocumento}
 */
public record TitularCommand(String nombres, String primerApellido, String segundoApellido, String tipoDocumento,
                             String numeroDocumento, String correo, String telefono) {

    /** Con el nombre completo en un solo texto, como lo envían los canales externos. */
    public TitularCommand(String nombre, String tipoDocumento, String numeroDocumento, String correo,
                          String telefono) {
        this(nombre, null, null, tipoDocumento, numeroDocumento, correo, telefono);
    }
}
