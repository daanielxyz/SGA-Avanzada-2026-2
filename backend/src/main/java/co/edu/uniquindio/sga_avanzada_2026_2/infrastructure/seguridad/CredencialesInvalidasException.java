package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

/**
 * Los datos para entrar no corresponden a una cuenta habilitada. El mensaje nunca dice cuál dato falló (CU-02).
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
