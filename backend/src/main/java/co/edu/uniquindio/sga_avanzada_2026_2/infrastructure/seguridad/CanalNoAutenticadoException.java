package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

/**
 * Un mensaje de canal externo sin la clave de ese canal, con una errada o de un canal que no existe (CAN-03 · DEC-73).
 * El mensaje es el mismo en los tres casos.
 */
public class CanalNoAutenticadoException extends RuntimeException {

    public CanalNoAutenticadoException() {
        super("El canal no se pudo autenticar: falta la clave del canal o no es válida");
    }
}
