package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.Canal;

/**
 * Salida de los casos de uso de Canal (DEC-31).
 *
 * @param referenciaCredencial nombre del secreto en la configuración externa, nunca el secreto (CAN-02 · DEC-47)
 */
public record CanalResult(String id, String alojamientoId, String nombre, String tipo, String referenciaCredencial,
                          boolean activo) {

    public static CanalResult de(Canal canal) {
        return new CanalResult(canal.id().valor(), canal.alojamientoId().valor(), canal.nombre(),
                canal.tipo().name(), canal.referenciaCredencial(), canal.activo());
    }
}
