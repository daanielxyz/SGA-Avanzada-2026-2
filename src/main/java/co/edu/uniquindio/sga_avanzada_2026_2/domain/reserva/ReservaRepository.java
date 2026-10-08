package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia del agregado Reserva.
 */
public interface ReservaRepository {

    void guardar(Reserva reserva);

    Optional<Reserva> buscarPorCodigo(ReservaId codigo);

    /**
     * Reservas PENDIENTE, CONFIRMADA o EN_CURSO del apartamento (EDO-03): lo que necesitan los servicios de
     * disponibilidad, bloqueo y baja.
     */
    List<Reserva> buscarActivasPorApartamento(ApartamentoId apartamentoId);

    /** La reserva que un canal externo ya envió con ese identificador, para no duplicarla (RN-19 · CAN-04). */
    Optional<Reserva> buscarPorCanalEIdExterno(CanalId canalId, String idExterno);
}
