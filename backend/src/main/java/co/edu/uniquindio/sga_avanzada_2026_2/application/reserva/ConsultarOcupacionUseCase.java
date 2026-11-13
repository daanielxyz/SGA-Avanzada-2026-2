package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Saldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso: calendario de ocupación por apartamento y rango de fechas, de 10 en 10 (CU-24 · sección 7.5 · DEC-18).
 * Cada reserva trae el nombre de su titular y su cuenta, para colorear el tablero (BL-09 · DEC-101).
 */
@Service
public class ConsultarOcupacionUseCase {

    private final ApartamentoRepository apartamentos;
    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final TitularRepository titulares;

    public ConsultarOcupacionUseCase(ApartamentoRepository apartamentos, ReservaRepository reservas,
                                     FolioRepository folios, TitularRepository titulares) {
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.folios = folios;
        this.titulares = titulares;
    }

    /**
     * @param hasta       exclusiva, como la salida de una estancia (EST-01)
     * @param apartamento opcional: solo ese apartamento
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el rango es inválido (EST-02)
     */
    @Transactional(readOnly = true)
    public Pagina<OcupacionResult> ejecutar(String alojamientoId, LocalDate desde, LocalDate hasta, String apartamento,
                                            int numeroPagina) {
        Estancia rango = new Estancia(desde, hasta);
        Pagina<Apartamento> pagina = Pagina.de(apartamentos.buscarActivos(new AlojamientoId(alojamientoId)).stream()
                .filter(a -> Optional.ofNullable(apartamento).map(a.codigo().valor()::equals).orElse(true))
                .sorted(Comparator.comparing(a -> a.codigo().valor()))
                .toList(), numeroPagina);
        Map<ApartamentoId, List<Reserva>> delTablero = pagina.contenido().stream()
                .collect(Collectors.toMap(Apartamento::codigo, a -> reservas.buscarDelTablero(a.codigo(), rango)));
        List<Reserva> todas = delTablero.values().stream().flatMap(List::stream).toList();
        Map<ReservaId, Folio> cuentas = folios.buscarPorReservas(todas.stream().map(Reserva::codigo).toList()).stream()
                .collect(Collectors.toMap(Folio::reservaId, Function.identity()));
        Map<TitularId, String> nombres = titulares
                .buscarPorIds(todas.stream().map(Reserva::titularId).distinct().toList()).stream()
                .collect(Collectors.toMap(Titular::id, Titular::nombre));
        return pagina.map(a -> fila(a, rango, delTablero.get(a.codigo()), cuentas, nombres));
    }

    private static OcupacionResult fila(Apartamento a, Estancia rango, List<Reserva> delTablero,
                                        Map<ReservaId, Folio> cuentas, Map<TitularId, String> nombres) {
        return new OcupacionResult(a.codigo().valor(), a.nombre(), a.estadoOperativo().name(),
                a.motivoFueraDeServicio().orElse(null),
                delTablero.stream()
                        .sorted(Comparator.comparing(r -> r.estancia().entrada()))
                        .map(r -> reservaOcupada(r, cuentas.get(r.codigo()), nombres.get(r.titularId())))
                        .toList(),
                a.bloqueosVigentesEn(rango).stream()
                        .map(b -> new OcupacionResult.BloqueoOcupado(b.id().valor(), b.fechaInicio(), b.fechaFin(),
                                b.motivo()))
                        .toList());
    }

    private static OcupacionResult.ReservaOcupada reservaOcupada(Reserva r, Folio folio, String titularNombre) {
        Optional<Folio> cuenta = Optional.ofNullable(folio);
        Optional<Saldo> saldo = cuenta.map(Folio::saldo);
        return new OcupacionResult.ReservaOcupada(r.codigo().valor(), r.estancia().entrada(), r.estancia().salida(),
                r.estado().name(), r.canalOrigen().name(), r.titularId().valor(), titularNombre,
                saldo.map(s -> s.monto().monto()).orElse(null),
                saldo.map(s -> s.situacion().name()).orElse(null),
                cuenta.map(f -> f.totalPagado().monto()).orElse(null));
    }
}
