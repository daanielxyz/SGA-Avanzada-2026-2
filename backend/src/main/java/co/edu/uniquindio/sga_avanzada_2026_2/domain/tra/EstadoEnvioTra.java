package co.edu.uniquindio.sga_avanzada_2026_2.domain.tra;

/** En qué va el envío del TRA de una reserva (DEC-130). */
public enum EstadoEnvioTra {
    /** Falta enviarlo: los datos todavía no están completos o aún no le ha tocado al planificador. */
    PENDIENTE,
    /** El Ministerio recibió al huésped principal y a todos los acompañantes. */
    ENVIADO,
    /** El último intento falló; se reintenta en la siguiente vuelta o a mano. */
    CON_ERROR
}
