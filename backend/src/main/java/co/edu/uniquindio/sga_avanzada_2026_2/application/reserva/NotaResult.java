package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.NotaReserva;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Una nota de la reserva para el siguiente turno, con el nombre vigente de quien la escribió y de quien la atendió
 * (BL-08 · DEC-105).
 *
 * @param autorNombre nulo si esa persona ya no existe
 * @param atencion    nula mientras está pendiente
 */
public record NotaResult(String id, String texto, String autor, String autorNombre, LocalDateTime escritaEn,
                         boolean atendida, AtencionResult atencion) {

    /** @param autorNombre nulo si esa persona ya no existe */
    public record AtencionResult(String autor, String autorNombre, LocalDateTime fechaHora) {
    }

    /** Las notas en el orden dado; cada nombre se busca una sola vez. */
    static List<NotaResult> de(List<NotaReserva> notas, DirectorioPersonal personal) {
        Map<UsuarioId, Optional<String>> nombres = new HashMap<>();
        return notas.stream().map(nota -> {
            AtencionResult atencion = Optional.ofNullable(nota.atencion())
                    .map(a -> new AtencionResult(a.autor().valor(), nombre(a.autor(), nombres, personal),
                            a.fechaHora()))
                    .orElse(null);
            return new NotaResult(nota.id().valor(), nota.texto(), nota.autor().valor(),
                    nombre(nota.autor(), nombres, personal), nota.escritaEn(), nota.atendida(), atencion);
        }).toList();
    }

    private static String nombre(UsuarioId id, Map<UsuarioId, Optional<String>> nombres,
                                 DirectorioPersonal personal) {
        return nombres.computeIfAbsent(id, personal::nombreDe).orElse(null);
    }
}
