package co.edu.uniquindio.sga_avanzada_2026_2.domain.canal;

/**
 * Por dónde llegó una reserva, conjunto cerrado de tres valores (CORI-01). No cambia las reglas de disponibilidad
 * (CORI-02).
 */
public enum CanalOrigen {
    PORTAL,
    DIRECTO,
    EXTERNO
}
