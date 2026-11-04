package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

/**
 * Cómo terminó una operación con el canal. Los rechazos por conflicto o duplicado también se registran: son los casos
 * que hay que poder demostrar (BIT-03).
 */
public enum ResultadoEvento {
    EXITOSO,
    DUPLICADO,
    CONFLICTO,
    RECHAZADO,
    ERROR
}
