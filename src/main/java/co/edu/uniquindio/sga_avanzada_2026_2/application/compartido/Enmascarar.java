package co.edu.uniquindio.sga_avanzada_2026_2.application.compartido;

/**
 * Oculta un dato personal al mostrarlo a quien todavía no probó que es su dueño (DEC-69).
 */
public final class Enmascarar {

    private Enmascarar() {
    }

    /** {@code ana.rios@correo.co} → {@code a***@correo.co}. */
    public static String correo(String correo) {
        return correo.charAt(0) + "***" + correo.substring(correo.indexOf('@'));
    }
}
