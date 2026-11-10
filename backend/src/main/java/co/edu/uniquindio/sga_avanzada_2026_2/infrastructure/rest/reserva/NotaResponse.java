package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.NotaResult;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JSON de una nota de la reserva para el siguiente turno (BL-08 · DEC-105). Solo la ve el personal.
 *
 * @param autor       quien la escribió
 * @param autorNombre su nombre vigente; nulo si esa persona ya no existe
 * @param atendida    {@code false} mientras está pendiente
 * @param atencion    quién la marcó como atendida y cuándo; nula si sigue pendiente
 */
public record NotaResponse(String id, String texto, String autor, String autorNombre, LocalDateTime escritaEn,
                           boolean atendida, AtencionResponse atencion) {

    /** @param autorNombre nulo si esa persona ya no existe */
    public record AtencionResponse(String autor, String autorNombre, LocalDateTime fechaHora) {
    }

    static NotaResponse de(NotaResult r) {
        return new NotaResponse(r.id(), r.texto(), r.autor(), r.autorNombre(), r.escritaEn(), r.atendida(),
                Optional.ofNullable(r.atencion())
                        .map(a -> new AtencionResponse(a.autor(), a.autorNombre(), a.fechaHora()))
                        .orElse(null));
    }
}
