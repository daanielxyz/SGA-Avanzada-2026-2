package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

/**
 * Al cambiar la contraseña desde el perfil, la actual no es la correcta (CU-04 · DEC-124). No es un 401: quien la
 * escribe ya tiene sesión, y un 401 le dice al cliente que la perdió.
 */
public class ContrasenaActualIncorrectaException extends RuntimeException {

    public ContrasenaActualIncorrectaException() {
        super("La contraseña actual no es correcta");
    }
}
