package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Salida de los casos de uso de ConflictoCanal (DEC-31): lo que pidió el canal, por qué se rechazó y, si ya se
 * revisó, quién, cuándo y qué decidió (CONF-03 · CONF-04).
 *
 * @param estado PENDIENTE o RESUELTO
 */
public record ConflictoResult(String id, String canalId, String idExterno, String apartamento, LocalDate entrada,
                              LocalDate salida, LocalDateTime detectadoEn, String motivo, String estado,
                              String resueltoPor, LocalDateTime resueltoEn, String decision) {

    public static ConflictoResult de(ConflictoCanal conflicto) {
        return new ConflictoResult(conflicto.id().valor(), conflicto.canalId().valor(), conflicto.idExterno(),
                conflicto.apartamentoId().valor(), conflicto.estanciaSolicitada().entrada(),
                conflicto.estanciaSolicitada().salida(), conflicto.detectadoEn(), conflicto.motivo(),
                conflicto.estado().name(),
                Optional.ofNullable(conflicto.resueltoPor()).map(UsuarioId::valor).orElse(null),
                conflicto.resueltoEn(), conflicto.decision());
    }
}
