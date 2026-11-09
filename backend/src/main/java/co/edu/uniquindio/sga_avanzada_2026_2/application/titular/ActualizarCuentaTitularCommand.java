package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

/**
 * Entrada de {@link ActualizarCuentaTitularUseCase}: lo que el huésped cambia desde su cuenta. Los apellidos pueden
 * llegar aparte o dentro de {@code nombres} (DEC-98).
 */
public record ActualizarCuentaTitularCommand(String id, String nombres, String primerApellido,
                                             String segundoApellido, String telefono) {

    /** Con el nombre completo en un solo texto. */
    public ActualizarCuentaTitularCommand(String id, String nombre, String telefono) {
        this(id, nombre, null, null, telefono);
    }
}
