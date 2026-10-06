package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

/**
 * Conjunto cerrado de tipos de cargo (CAR-02 · TCAR-01); agregar uno es un cambio del dominio (TCAR-04).
 */
public enum TipoCargo {
    HOSPEDAJE,
    SERVICIO_ADICIONAL,
    PENALIDAD_CANCELACION, // también la del no-show (CAR-02)
    AJUSTE
}
