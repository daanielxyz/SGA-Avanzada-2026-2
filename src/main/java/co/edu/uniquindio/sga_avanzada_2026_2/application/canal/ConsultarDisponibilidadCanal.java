package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ApartamentoDisponibleResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarDisponibles;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarDisponiblesCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.OperacionCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ResultadoEvento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.SentidoEvento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: un canal externo consulta qué apartamentos de su alojamiento están libres para unas fechas y un grupo
 * (CU-13 · CAN-03 · DEC-73). Usa la misma búsqueda del portal (CU-07) y deja la consulta en la bitácora (BIT-01).
 */
@Service
public class ConsultarDisponibilidadCanal {

    private final CanalRepository canales;
    private final EventoCanalRepository eventos;
    private final BuscarDisponibles buscarDisponibles;
    private final GeneradorCodigos codigos;
    private final Clock reloj;

    public ConsultarDisponibilidadCanal(CanalRepository canales, EventoCanalRepository eventos,
                                        BuscarDisponibles buscarDisponibles, GeneradorCodigos codigos, Clock reloj) {
        this.canales = canales;
        this.eventos = eventos;
        this.buscarDisponibles = buscarDisponibles;
        this.codigos = codigos;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el canal
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el canal no es
     *         externo o está inactivo (CAN-05), o las fechas no forman una estancia (EST-01)
     */
    @Transactional
    public List<ApartamentoDisponibleResult> ejecutar(ConsultarDisponibilidadCanalCommand comando) {
        CanalId canalId = new CanalId(comando.canalId());
        Canal canal = canales.buscarPorId(canalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el canal", canalId.valor()));
        canal.exigirReservasExternas();

        List<ApartamentoDisponibleResult> libres = buscarDisponibles.ejecutar(new BuscarDisponiblesCommand(
                canal.alojamientoId().valor(), comando.entrada(), comando.salida(), comando.ocupantes()));

        eventos.registrar(new EventoCanal(new EventoCanalId(codigos.siguiente(SerieCodigo.EVENTO)), canal.id(),
                OperacionCanal.CONSULTAR_DISPONIBILIDAD, SentidoEvento.ENTRANTE, LocalDateTime.now(reloj),
                comando.cargaUtil(), ResultadoEvento.EXITOSO, libres.size() + " apartamentos disponibles"));
        return libres;
    }
}
