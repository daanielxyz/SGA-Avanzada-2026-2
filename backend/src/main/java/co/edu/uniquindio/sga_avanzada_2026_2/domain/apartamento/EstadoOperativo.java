package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import java.util.Objects;

/**
 * Estado físico actual del apartamento (EOPE-01), independiente del bloqueo (BLO-05).
 * Quién puede hacer cada cambio (EOPE-04) se controla en la aplicación, no aquí.
 */
public enum EstadoOperativo {
    PREPARADO("preparado"),
    OCUPADO("ocupado"),
    PENDIENTE_PREPARACION("pendiente de preparación"),
    EN_PREPARACION("en preparación"),
    FUERA_DE_SERVICIO("fuera de servicio");

    private final String texto;

    EstadoOperativo(String texto) {
        this.texto = texto;
    }

    /** Cómo se nombra en un mensaje: «El apartamento está en preparación» (#8 · DEC-118). */
    public String texto() {
        return texto;
    }

    /**
     * Transiciones válidas (EOPE-02): PREPARADO→OCUPADO (registro), OCUPADO→PENDIENTE_PREPARACION (salida),
     * PENDIENTE_PREPARACION→EN_PREPARACION→PREPARADO (servicio), FUERA_DE_SERVICIO desde cualquier estado no
     * ocupado, y FUERA_DE_SERVICIO→PENDIENTE_PREPARACION para volver al ciclo (DEC-21).
     *
     * @param destino estado al que se quiere pasar
     * @return {@code true} si la transición está permitida
     */
    public boolean puedePasarA(EstadoOperativo destino) {
        Objects.requireNonNull(destino, "destino");
        if (destino == FUERA_DE_SERVICIO) {
            return this != OCUPADO && this != FUERA_DE_SERVICIO;
        }
        return switch (this) {
            case PREPARADO -> destino == OCUPADO;
            case OCUPADO -> destino == PENDIENTE_PREPARACION;
            case PENDIENTE_PREPARACION -> destino == EN_PREPARACION;
            case EN_PREPARACION -> destino == PREPARADO;
            case FUERA_DE_SERVICIO -> destino == PENDIENTE_PREPARACION;
        };
    }

    /**
     * Solo un apartamento PREPARADO puede recibir un grupo (RN-11 · EOPE-03).
     *
     * @return {@code true} si se permite el registro de llegada
     */
    public boolean permiteRegistro() {
        return this == PREPARADO;
    }
}
