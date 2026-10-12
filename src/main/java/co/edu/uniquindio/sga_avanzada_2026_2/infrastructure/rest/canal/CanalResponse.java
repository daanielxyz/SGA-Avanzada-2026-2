package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.CanalResult;

/**
 * JSON de un canal de venta (DEC-31).
 *
 * @param referenciaCredencial nombre del secreto en la configuración externa, nunca el secreto (CAN-02 · DEC-47)
 */
public record CanalResponse(String id, String nombre, String tipo, String referenciaCredencial, boolean activo) {

    static CanalResponse de(CanalResult r) {
        return new CanalResponse(r.id(), r.nombre(), r.tipo(), r.referenciaCredencial(), r.activo());
    }
}
