package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado NotaReserva. No hay eliminación: una nota resuelta se marca como atendida
 * (BL-08 · DEC-105).
 */
public interface NotaReservaRepository {

    void guardar(NotaReserva nota);

    Optional<NotaReserva> buscarPorId(NotaReservaId id);

    /** Las notas de la reserva, de la más reciente a la más antigua; son pocas, así que no se paginan. */
    List<NotaReserva> listarPorReserva(ReservaId reservaId);
}
