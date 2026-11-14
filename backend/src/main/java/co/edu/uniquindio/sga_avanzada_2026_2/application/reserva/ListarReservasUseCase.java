package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.FiltroReservas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso: recepción busca reservas con filtros (CU-25 · DEC-72), de la más reciente a la más antigua.
 * El huésped no lo usa: ve las suyas con {@link ListarReservasDeTitularUseCase}.
 */
@Service
public class ListarReservasUseCase {

    private final ReservaRepository reservas;
    private final TitularRepository titulares;

    public ListarReservasUseCase(ReservaRepository reservas, TitularRepository titulares) {
        this.reservas = reservas;
        this.titulares = titulares;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la fecha desde no es
     *         anterior a la fecha hasta
     */
    @Transactional(readOnly = true)
    public Pagina<ReservaConTitularResult> ejecutar(ListarReservasCommand comando) {
        FiltroReservas filtro = new FiltroReservas(comando.desde(), comando.hasta(),
                aConjunto(comando.estados(), EstadoReserva::valueOf),
                aConjunto(comando.apartamentos(), ApartamentoId::new),
                aConjunto(comando.canales(), CanalOrigen::valueOf),
                Optional.ofNullable(comando.titular()).map(TitularId::new).orElse(null));
        // A lo sumo 10 consultas por clave primaria: el tamaño de la página (DEC-18)
        return reservas.buscar(filtro, comando.pagina()).map(reserva -> new ReservaConTitularResult(
                ReservaResult.de(reserva),
                titulares.buscarPorId(reserva.titularId()).map(Titular::nombre).orElse(null)));
    }

    /** #22 · DEC-119: un filtro que no llegó es un conjunto vacío, «cualquiera». */
    private static <T> Set<T> aConjunto(List<String> valores, Function<String, T> convertir) {
        return Optional.ofNullable(valores).orElse(List.of()).stream().map(convertir).collect(Collectors.toSet());
    }
}
