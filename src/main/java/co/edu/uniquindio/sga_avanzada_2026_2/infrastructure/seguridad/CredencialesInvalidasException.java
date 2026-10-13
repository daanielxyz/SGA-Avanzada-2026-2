package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

/**
 * El correo o la contraseña no corresponden a un usuario habilitado. El mensaje nunca dice cuál de los dos falló
 * (CU-02).
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }
}
