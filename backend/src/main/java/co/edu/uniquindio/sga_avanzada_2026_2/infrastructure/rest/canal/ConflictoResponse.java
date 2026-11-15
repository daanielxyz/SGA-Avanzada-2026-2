package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.ConflictoResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JSON de un conflicto de canal (DEC-31): lo que pidió el canal, por qué se rechazó y, si ya se revisó, quién,
 * cuándo y qué decidió (CONF-03 · CONF-04).
 *
 * @param estado PENDIENTE o RESUELTO
 * @param salida exclusiva (EST-01)
 * @param titularNombre a quién llamar por el conflicto (DEC-129); con el teléfono y el correo, null en los
 *                      conflictos de antes o si el canal no mandó ese dato
 */
public record ConflictoResponse(String id, String canalId, String idExterno, String apartamento, LocalDate entrada,
                                LocalDate salida, LocalDateTime detectadoEn, String motivo, String titularNombre,
                                String titularTelefono, String titularCorreo, String estado,
                                String resueltoPor, LocalDateTime resueltoEn, String decision) {

    static ConflictoResponse de(ConflictoResult r) {
        return new ConflictoResponse(r.id(), r.canalId(), r.idExterno(), r.apartamento(), r.entrada(), r.salida(),
                r.detectadoEn(), r.motivo(), r.titularNombre(), r.titularTelefono(), r.titularCorreo(), r.estado(), r.resueltoPor(), r.resueltoEn(), r.decision());
    }
}
