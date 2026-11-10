package co.edu.uniquindio.sga_avanzada_2026_2.domain.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado Folio.
 */
public interface FolioRepository {

    void guardar(Folio folio);

    /** El folio de una reserva: la relación es uno a uno (FOL-02). */
    Optional<Folio> buscarPorReserva(ReservaId reservaId);

    /** Los folios de varias reservas de una vez, para el tablero (BL-09). */
    List<Folio> buscarPorReservas(Collection<ReservaId> reservaIds);
}
