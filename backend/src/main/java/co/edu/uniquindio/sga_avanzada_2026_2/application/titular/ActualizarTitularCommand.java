package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

/**
 * Entrada de {@link ActualizarTitularUseCase}: nombre y contacto. El documento no se puede cambiar (TIT-05). Los
 * apellidos pueden llegar aparte o dentro de {@code nombres} (DEC-98).
 */
public record ActualizarTitularCommand(String id, String nombres, String primerApellido, String segundoApellido,
                                       String correo, String telefono) {

    /** Con el nombre completo en un solo texto. */
    public ActualizarTitularCommand(String id, String nombre, String correo, String telefono) {
        this(id, nombre, null, null, correo, telefono);
    }
}
