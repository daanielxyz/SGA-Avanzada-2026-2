package co.edu.uniquindio.sga_avanzada_2026_2.domain.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;

import java.util.List;
import java.util.Optional;

/** Puerto del agregado EnvioTra (DEC-130): uno por reserva. */
public interface EnvioTraRepository {

    void guardar(EnvioTra envio);

    Optional<EnvioTra> buscarPorReserva(ReservaId reserva);

    /** Los que el planificador debe intentar: PENDIENTE, o CON_ERROR con menos de {@code maximoIntentos}. */
    List<ReservaId> buscarParaEnviar(int maximoIntentos);

    /** Los de un estado, del intento más reciente al más antiguo, de 10 en 10. */
    Pagina<EnvioTra> listarPorEstado(EstadoEnvioTra estado, int numeroPagina);
}
