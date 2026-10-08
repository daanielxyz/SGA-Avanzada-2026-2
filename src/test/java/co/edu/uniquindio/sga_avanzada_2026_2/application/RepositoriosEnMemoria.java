package co.edu.uniquindio.sga_avanzada_2026_2.application;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Ubicacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementaciones falsas de los puertos para probar casos de uso sin Spring ni base de datos.
 */
public final class RepositoriosEnMemoria {

    /** Hoy en las pruebas de casos de uso: 1 de octubre de 2026 a las 10:00 en Colombia. */
    public static final Clock RELOJ = Clock.fixed(
            LocalDate.of(2026, 10, 1).atTime(10, 0).atZone(ZoneId.of("America/Bogota")).toInstant(),
            ZoneId.of("America/Bogota"));

    private RepositoriosEnMemoria() {
    }

    public static final class Apartamentos implements ApartamentoRepository {

        public final Map<ApartamentoId, Apartamento> guardados = new LinkedHashMap<>();

        @Override
        public void guardar(Apartamento apartamento) {
            guardados.put(apartamento.codigo(), apartamento);
        }

        @Override
        public Optional<Apartamento> buscarPorCodigo(ApartamentoId codigo) {
            return Optional.ofNullable(guardados.get(codigo));
        }

        @Override
        public Pagina<Apartamento> listarPorAlojamiento(AlojamientoId alojamientoId, int numeroPagina) {
            List<Apartamento> todos = guardados.values().stream()
                    .filter(a -> a.alojamientoId().equals(alojamientoId))
                    .sorted(Comparator.comparing(a -> a.codigo().valor()))
                    .toList();
            List<Apartamento> pagina = todos.stream()
                    .skip((long) numeroPagina * Pagina.TAMANO)
                    .limit(Pagina.TAMANO)
                    .toList();
            return new Pagina<>(pagina, numeroPagina, Pagina.TAMANO, todos.size());
        }

        @Override
        public List<Apartamento> buscarActivos(AlojamientoId alojamientoId) {
            return guardados.values().stream()
                    .filter(a -> a.alojamientoId().equals(alojamientoId) && a.activo())
                    .toList();
        }
    }

    public static final class Alojamientos implements AlojamientoRepository {

        public final Map<AlojamientoId, Alojamiento> guardados = new LinkedHashMap<>();

        @Override
        public void guardar(Alojamiento alojamiento) {
            guardados.put(alojamiento.id(), alojamiento);
        }

        @Override
        public Optional<Alojamiento> buscarPorId(AlojamientoId id) {
            return Optional.ofNullable(guardados.get(id));
        }
    }

    public static final class Politicas implements PoliticaCancelacionRepository {

        public final Map<PoliticaId, PoliticaCancelacion> guardadas = new LinkedHashMap<>();

        @Override
        public void guardar(PoliticaCancelacion politica) {
            guardadas.put(politica.id(), politica);
        }

        @Override
        public Optional<PoliticaCancelacion> buscarPorId(PoliticaId id) {
            return Optional.ofNullable(guardadas.get(id));
        }

        @Override
        public Optional<PoliticaCancelacion> buscarVigente(AlojamientoId alojamientoId) {
            return guardadas.values().stream()
                    .filter(p -> p.alojamientoId().equals(alojamientoId))
                    .max(Comparator.comparingInt(PoliticaCancelacion::version));
        }
    }

    public static final class Calendarios implements CalendarioTemporadasRepository {

        public final Map<AlojamientoId, CalendarioTemporadas> guardados = new LinkedHashMap<>();

        @Override
        public void guardar(CalendarioTemporadas calendario) {
            guardados.put(calendario.alojamientoId(), calendario);
        }

        @Override
        public Optional<CalendarioTemporadas> buscarPorAlojamiento(AlojamientoId alojamientoId) {
            return Optional.ofNullable(guardados.get(alojamientoId));
        }
    }

    public static final class Tarifas implements TarifaRepository {

        public final List<Tarifa> guardadas = new ArrayList<>();

        @Override
        public void guardar(Tarifa tarifa) {
            guardadas.add(tarifa);
        }

        @Override
        public List<Tarifa> buscarVigentesPorApartamento(ApartamentoId apartamentoId, LocalDate fecha) {
            return guardadas.stream()
                    .filter(t -> t.apartamentoId().equals(apartamentoId) && !t.vigenteDesde().isAfter(fecha))
                    .collect(Collectors.groupingBy(Tarifa::temporadaId,
                            Collectors.maxBy(Comparator.comparingInt(Tarifa::version))))
                    .values().stream()
                    .flatMap(Optional::stream)
                    .toList();
        }
    }

    public static final class Reservas implements ReservaRepository {

        public final Map<ReservaId, Reserva> guardadas = new LinkedHashMap<>();

        @Override
        public void guardar(Reserva reserva) {
            guardadas.put(reserva.codigo(), reserva);
        }

        @Override
        public Optional<Reserva> buscarPorCodigo(ReservaId codigo) {
            return Optional.ofNullable(guardadas.get(codigo));
        }

        @Override
        public List<Reserva> buscarActivasPorApartamento(ApartamentoId apartamentoId) {
            return guardadas.values().stream()
                    .filter(r -> r.apartamentoId().equals(apartamentoId) && r.estaActiva())
                    .toList();
        }

        @Override
        public Optional<Reserva> buscarPorCanalEIdExterno(CanalId canalId, String idExterno) {
            return guardadas.values().stream()
                    .filter(r -> Objects.equals(r.canalId(), canalId) && Objects.equals(r.idExterno(), idExterno))
                    .findFirst();
        }

        @Override
        public List<Reserva> buscarPendientesCreadasAntesDe(LocalDateTime limite) {
            return guardadas.values().stream()
                    .filter(r -> r.estado() == EstadoReserva.PENDIENTE && r.creadaEn().isBefore(limite))
                    .sorted(Comparator.comparing(Reserva::creadaEn))
                    .toList();
        }
    }

    public static final class Folios implements FolioRepository {

        public final Map<ReservaId, Folio> guardados = new LinkedHashMap<>();

        @Override
        public void guardar(Folio folio) {
            guardados.put(folio.reservaId(), folio);
        }

        @Override
        public Optional<Folio> buscarPorReserva(ReservaId reservaId) {
            return Optional.ofNullable(guardados.get(reservaId));
        }
    }

    public static final class Titulares implements TitularRepository {

        public final Map<TitularId, Titular> guardados = new LinkedHashMap<>();

        @Override
        public void guardar(Titular titular) {
            guardados.put(titular.id(), titular);
        }

        @Override
        public Optional<Titular> buscarPorId(TitularId id) {
            return Optional.ofNullable(guardados.get(id));
        }

        @Override
        public Optional<Titular> buscarPorDocumento(AlojamientoId alojamientoId, Documento documento) {
            return guardados.values().stream()
                    .filter(t -> t.alojamientoId().equals(alojamientoId) && t.documento().equals(documento))
                    .findFirst();
        }
    }

    /** Numera cada serie desde 1, como las secuencias de la BD. */
    public static final class Codigos implements GeneradorCodigos {

        private final Map<SerieCodigo, Long> ultimos = new EnumMap<>(SerieCodigo.class);

        @Override
        public String siguiente(SerieCodigo serie) {
            return serie.formatear(ultimos.merge(serie, 1L, Long::sum), LocalDate.now(RELOJ));
        }
    }

    public static ParametrosAlojamiento parametros(int minimoCapacidadesDistintas) {
        return new ParametrosAlojamiento(12, LocalTime.of(15, 0), LocalTime.of(11, 0), Duration.ofHours(3),
                Duration.ofHours(24), LocalTime.of(22, 0), new Porcentaje(30), 2, 1, 2, 2,
                minimoCapacidadesDistintas);
    }

    public static Alojamiento puertaAlSol(String id) {
        return puertaAlSol(id, parametros(2));
    }

    public static Alojamiento puertaAlSol(String id, ParametrosAlojamiento parametros) {
        return new Alojamiento(new AlojamientoId(id), "Puerta al Sol", "Apartamentos frente al mar", "Santa Marta",
                "Calle 1 # 2-3", new Ubicacion(11.24, -74.21), null, parametros,
                List.of(new ServicioAdicional(new ServicioAdicionalId("SRV-1"), "Desayuno", true, Dinero.de(25_000),
                        true)),
                List.of(new MedioPago("EFECTIVO"), new MedioPago("TARJETA")));
    }
}
