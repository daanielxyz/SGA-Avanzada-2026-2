package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.FiltroReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Caso de uso: recepción busca reservas con filtros (CU-25 · DEC-72), de la que entra primero a la que entra después.
 * El huésped no lo usa: ve las suyas con {@link ListarReservasDeTitular}.
 */
@Service
public class ListarReservas {

    private final ReservaRepository reservas;

    public ListarReservas(ReservaRepository reservas) {
        this.reservas = reservas;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la fecha desde no es
     *         anterior a la fecha hasta
     */
    @Transactional(readOnly = true)
    public Pagina<ReservaResult> ejecutar(ListarReservasCommand comando) {
        FiltroReservas filtro = new FiltroReservas(comando.desde(), comando.hasta(),
                Optional.ofNullable(comando.estado()).map(EstadoReserva::valueOf).orElse(null),
                Optional.ofNullable(comando.apartamento()).map(ApartamentoId::new).orElse(null),
                Optional.ofNullable(comando.canal()).map(CanalOrigen::valueOf).orElse(null),
                Optional.ofNullable(comando.titular()).map(TitularId::new).orElse(null));
        return reservas.buscar(filtro, comando.pagina()).map(ReservaResult::de);
    }
}
