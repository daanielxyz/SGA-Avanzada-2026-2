package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

/**
 * El asistente con IA no pudo responder (DEC-127). No es una regla de negocio rota ni llega al cliente: el caso de uso
 * responde con el manual y dice por qué.
 */
public class AsistenteNoDisponibleException extends RuntimeException {

    private final MotivoSinIa motivo;

    public AsistenteNoDisponibleException(MotivoSinIa motivo, String detalle) {
        super(detalle);
        this.motivo = motivo;
    }

    public MotivoSinIa motivo() {
        return motivo;
    }
}
