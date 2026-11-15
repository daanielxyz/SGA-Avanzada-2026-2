package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ContactoTitular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

/**
 * Salida de los casos de uso de ConflictoCanal (DEC-31): lo que pidió el canal, por qué se rechazó y, si ya se
 * revisó, quién, cuándo y qué decidió (CONF-03 · CONF-04).
 *
 * @param titularNombre   a quién llamar (DEC-129); con {@code titularTelefono} y {@code titularCorreo}, null en los
 *                        conflictos de antes de DEC-129 o si el canal no mandó ese dato
 * @param estado PENDIENTE o RESUELTO
 */
public record ConflictoResult(String id, String canalId, String idExterno, String apartamento, LocalDate entrada,
                              LocalDate salida, LocalDateTime detectadoEn, String motivo, String titularNombre,
                              String titularTelefono, String titularCorreo, String estado,
                              String resueltoPor, LocalDateTime resueltoEn, String decision) {

    public static ConflictoResult de(ConflictoCanal conflicto) {
        return new ConflictoResult(conflicto.id().valor(), conflicto.canalId().valor(), conflicto.idExterno(),
                conflicto.apartamentoId().valor(), conflicto.estanciaSolicitada().entrada(),
                conflicto.estanciaSolicitada().salida(), conflicto.detectadoEn(), conflicto.motivo(),
                contacto(conflicto, ContactoTitular::nombre), contacto(conflicto, ContactoTitular::telefono),
                contacto(conflicto, ContactoTitular::correo), conflicto.estado().name(),
                Optional.ofNullable(conflicto.resueltoPor()).map(UsuarioId::valor).orElse(null),
                conflicto.resueltoEn(), conflicto.decision());
    }

    private static String contacto(ConflictoCanal conflicto, Function<ContactoTitular, String> dato) {
        return Optional.ofNullable(conflicto.contacto()).map(dato).orElse(null);
    }
}
