package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

/**
 * Objeto de valor (Enum): Estado operativo de un apartamento.
 */
public enum EstadoOperativo {
    PREPARADO,
    OCUPADO,
    PENDIENTE_PREPARACION,
    EN_PREPARACION,
    FUERA_DE_SERVICIO;

    /**
     * Evalúa si una transición hacia otro estado operativo es válida.
     */
    public boolean puedePasarA(EstadoOperativo destino) {
        if (destino == null || this == destino) {
            return false;
        }
        return switch (this) {
            case PREPARADO -> destino == OCUPADO || destino == PENDIENTE_PREPARACION || destino == FUERA_DE_SERVICIO;
            case OCUPADO -> destino == PENDIENTE_PREPARACION;
            case PENDIENTE_PREPARACION -> destino == EN_PREPARACION || destino == FUERA_DE_SERVICIO;
            case EN_PREPARACION -> destino == PREPARADO || destino == FUERA_DE_SERVICIO;
            case FUERA_DE_SERVICIO -> destino == PENDIENTE_PREPARACION || destino == PREPARADO;
        };
    }
}
