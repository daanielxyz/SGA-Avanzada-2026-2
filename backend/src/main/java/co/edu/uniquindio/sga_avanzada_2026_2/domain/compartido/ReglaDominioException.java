package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import java.util.Optional;

/**
 * Base de toda violación de una regla de negocio (se traduce a 409 en el borde).
 */
public class ReglaDominioException extends RuntimeException {

    private final MotivoRechazo motivo;

    public ReglaDominioException(String mensaje) {
        this(null, mensaje);
    }

    /**
     * @param motivo por qué se rechazó, para las reglas ante las que una pantalla reacciona distinto
     */
    public ReglaDominioException(MotivoRechazo motivo, String mensaje) {
        super(mensaje);
        this.motivo = motivo;
    }

    public Optional<MotivoRechazo> motivo() {
        return Optional.ofNullable(motivo);
    }
}
