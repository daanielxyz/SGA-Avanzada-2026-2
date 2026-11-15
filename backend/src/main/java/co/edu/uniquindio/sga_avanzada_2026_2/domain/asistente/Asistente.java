package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

/**
 * Puerto del asistente de dudas del PMS (DEC-127). Hay una implementación con IA y una local, que responde con el
 * manual. La operación nunca depende de este puerto: si la IA no puede responder, lanza
 * {@link AsistenteNoDisponibleException} y responde la local.
 */
public interface Asistente {

    /**
     * @throws AsistenteNoDisponibleException si no puede responder ahora (cuota, límite, falla o tiempo agotado)
     */
    Respuesta responder(Consulta consulta);
}
