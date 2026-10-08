package co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Ciclo de vida de una novedad: ABIERTA → EN_REVISION → CERRADA (NOV-04). Una cerrada no se reabre ni se elimina
 * (NOV-06).
 */
public enum EstadoNovedad {
    ABIERTA,
    EN_REVISION,
    CERRADA;

    /**
     * El paso siguiente del ciclo (NOV-04).
     *
     * @throws ReglaDominioException si ya está CERRADA
     */
    public EstadoNovedad siguiente() {
        return switch (this) {
            case ABIERTA -> EN_REVISION;
            case EN_REVISION -> CERRADA;
            case CERRADA -> throw new ReglaDominioException("Una novedad cerrada no avanza más");
        };
    }
}
