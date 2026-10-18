package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;

import java.time.LocalDateTime;
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

    /**
     * Reservas PENDIENTE creadas estrictamente antes de {@code limite} (= ahora − plazo de confirmación): las que ya
     * vencieron (RN-21). Las usa el planificador (B4).
     */
    List<Reserva> buscarPendientesCreadasAntesDe(LocalDateTime limite);

    /**
     * Reservas del titular, de la más reciente a la más antigua, de 10 en 10: las que ve el huésped en su cuenta
     * (USU-04 · DEC-18).
     *
     * @param numeroPagina empieza en 0
     */
    Pagina<Reserva> listarPorTitular(TitularId titularId, int numeroPagina);

    /**
     * Las reservas que cumplen todos los filtros, de la que entra primero a la que entra después, de 10 en 10
     * (CU-25 · DEC-18 · DEC-72).
     *
     * @param numeroPagina empieza en 0
     */
    Pagina<Reserva> buscar(FiltroReservas filtro, int numeroPagina);
}
